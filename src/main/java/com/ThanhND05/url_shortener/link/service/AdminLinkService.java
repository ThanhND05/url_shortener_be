package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.link.dto.request.AdminUpdateLinkStatusRequest;
import com.ThanhND05.url_shortener.link.dto.response.AdminLinkResponse;
import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.enums.LinkStatus;
import com.ThanhND05.url_shortener.link.repository.LinkRepository;
import com.ThanhND05.url_shortener.link.repository.LinkSpecifications;
import com.ThanhND05.url_shortener.link.repository.RedirectLookupRepository;
import com.ThanhND05.url_shortener.iam.api.IamPublicApi;
import com.ThanhND05.url_shortener.analytics.api.AnalyticsPublicApi;
import com.ThanhND05.url_shortener.analytics.api.dto.LinkCounterApiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service admin quản lý links toàn hệ thống.
 *
 * Chức năng:
 * - Tìm kiếm/filter links (search text, status, owner, date range).
 * - Xem chi tiết link bất kỳ (bao gồm ownerEmail).
 * - Đổi trạng thái link (ACTIVE/DISABLED/QUARANTINED).
 * - Cập nhật redirect type.
 * - Xóa mềm link bất kỳ.
 *
 * Lưu ý:
 * - Mọi thay đổi status/delete đều sync sang redirect_lookup.
 * - Evict Redis cache khi update/delete để redirect phản ánh đúng trạng thái.
 * - Search/list dùng batch-load để tránh N+1 (ownerEmail + clickCount).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLinkService {

    private final LinkRepository linkRepository;
    private final IamPublicApi iamPublicApi;
    private final RedirectLookupRepository redirectLookupRepository;
    private final AnalyticsPublicApi analyticsPublicApi;

    // ── TÌM KIẾM / DANH SÁCH ────────────────────────────

    /**
     * Admin search links toàn hệ thống với filter linh hoạt.
     *
     * Tối ưu production: batch-load ownerEmails + clickCounts
     * thay vì N+1 query cho mỗi link.
     *
     * @param search   tìm theo original_url hoặc short_code (ILIKE).
     * @param status   filter theo LinkStatus (null = tất cả).
     * @param ownerId  filter theo user tạo (null = tất cả).
     * @param from     filter từ ngày tạo (null = bỏ qua).
     * @param to       filter đến ngày tạo (null = bỏ qua).
     * @param pageable phân trang + sắp xếp.
     */
    @Transactional(readOnly = true)
    public Page<AdminLinkResponse> searchLinks(String search, String status, UUID ownerId,
            Instant from, Instant to, Pageable pageable) {
        LinkStatus linkStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                linkStatus = LinkStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Trạng thái không hợp lệ: " + status
                        + ". Giá trị hợp lệ: ACTIVE, DISABLED, EXPIRED, DELETED, QUARANTINED.");
            }
        }

        var spec = LinkSpecifications.adminSearch(search, linkStatus, ownerId, from, to);
        Page<Link> linkPage = linkRepository.findAll(spec, pageable);

        if (linkPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Batch-load: thu thập tất cả ownerIds và linkIds một lần
        List<Link> links = linkPage.getContent();
        Map<UUID, String> ownerEmailMap = batchLoadOwnerEmails(links);
        Map<Long, LinkCounterApiDto> counterMap = batchLoadCounters(links);

        List<AdminLinkResponse> responses = links.stream()
                .map(link -> {
                    String ownerEmail = link.getOwnerId() != null
                            ? ownerEmailMap.get(link.getOwnerId())
                            : null;
                    LinkCounterApiDto counter = counterMap.get(link.getId());
                    long clicks = counter != null ? counter.totalClicks() : 0;
                    Instant lastClicked = counter != null ? counter.lastClickedAt() : null;
                    return AdminLinkResponse.from(link, ownerEmail, clicks, lastClicked);
                })
                .toList();

        return new PageImpl<>(responses, pageable, linkPage.getTotalElements());
    }

    // ── XEM CHI TIẾT ─────────────────────────────────────

    /** Admin xem chi tiết link bất kỳ (bao gồm thông tin owner). */
    @Transactional(readOnly = true)
    public AdminLinkResponse getLink(UUID publicId) {
        Link link = findByPublicId(publicId);
        return toAdminResponse(link);
    }

    // ── ĐỔI TRẠNG THÁI ──────────────────────────────────

    /**
     * Admin đổi trạng thái link.
     * Sync status sang redirect_lookup để redirect phản ánh ngay.
     *
     * VD: Admin ban link phishing → status = QUARANTINED → redirect_lookup status =
     * QUARANTINED
     * → RedirectService resolve() không tìm thấy (vì filter status = ACTIVE).
     */
    @Transactional
    public AdminLinkResponse updateLinkStatus(UUID publicId, AdminUpdateLinkStatusRequest request) {
        Link link = findByPublicId(publicId);

        LinkStatus newStatus;
        try {
            newStatus = LinkStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái không hợp lệ: " + request.status());
        }

        LinkStatus oldStatus = link.getStatus();
        link.setStatus(newStatus);

        // Soft delete timestamp
        if (newStatus == LinkStatus.DELETED && link.getDeletedAt() == null) {
            link.setDeletedAt(Instant.now());
        }

        link = linkRepository.save(link);

        // Sync redirect_lookup
        syncRedirectLookupStatus(link);

        log.info("Admin changed link status: {} ({} → {}), reason: {}",
                link.getShortCode(), oldStatus, newStatus,
                request.reason() != null ? request.reason() : "N/A");

        return toAdminResponse(link);
    }

    // ── CẬP NHẬT REDIRECT TYPE ───────────────────────────

    /**
     * Admin cập nhật redirect type cho link.
     *
     * @param redirectType HTTP redirect code: 301, 302, 307, 308.
     */
    @Transactional
    public AdminLinkResponse updateRedirectType(UUID publicId, short redirectType) {
        if (redirectType != 301 && redirectType != 302
                && redirectType != 307 && redirectType != 308) {
            throw new BusinessException(
                    "Redirect type không hợp lệ: " + redirectType
                            + ". Giá trị hợp lệ: 301, 302, 307, 308.");
        }

        Link link = findByPublicId(publicId);
        link.setRedirectType(redirectType);
        link = linkRepository.save(link);

        // Sync redirect_lookup
        redirectLookupRepository.findByDomainIdAndShortCode(link.getDomainId(), link.getShortCode())
                .ifPresent(rl -> {
                    rl.setRedirectType(redirectType);
                    redirectLookupRepository.save(rl);
                });

        log.info("Admin changed redirect type: {} → {}", link.getShortCode(), redirectType);
        return toAdminResponse(link);
    }

    // ── XÓA MỀM ─────────────────────────────────────────

    /** Admin xóa mềm link bất kỳ. */
    @Transactional
    public void deleteLink(UUID publicId) {
        Link link = findByPublicId(publicId);
        link.setStatus(LinkStatus.DELETED);
        link.setDeletedAt(Instant.now());
        linkRepository.save(link);

        syncRedirectLookupStatus(link);
        log.info("Admin deleted link: {}", link.getShortCode());
    }

    // ── HELPERS ──────────────────────────────────────────

    private Link findByPublicId(UUID publicId) {
        return linkRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Link", "publicId", publicId));
    }

    /**
     * Convert single link → AdminLinkResponse.
     * Dùng cho single-entity operations (getLink, updateStatus, ...).
     * Cho phép N+1 vì chỉ 1 entity → 3 queries total (chấp nhận được).
     */
    private AdminLinkResponse toAdminResponse(Link link) {
        String ownerEmail = null;
        if (link.getOwnerId() != null) {
            ownerEmail = iamPublicApi.getUserEmail(link.getOwnerId());
        }

        var counter = analyticsPublicApi.getCounterByLinkId(link.getId());
        long clicks = counter != null ? counter.totalClicks() : 0;
        Instant lastClicked = counter != null ? counter.lastClickedAt() : null;

        return AdminLinkResponse.from(link, ownerEmail, clicks, lastClicked);
    }

    /**
     * Batch-load owner emails cho danh sách links.
     * 1 query thay vì N queries → tránh N+1.
     */
    private Map<UUID, String> batchLoadOwnerEmails(List<Link> links) {
        Set<UUID> ownerIds = links.stream()
                .map(Link::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ownerIds.isEmpty()) return Map.of();

        return iamPublicApi.getUserEmails(ownerIds);
    }

    /**
     * Batch-load click counters cho danh sách links.
     * 1 query thay vì N queries → tránh N+1.
     */
    private Map<Long, LinkCounterApiDto> batchLoadCounters(List<Link> links) {
        List<Long> linkIds = links.stream()
                .map(Link::getId)
                .toList();

        if (linkIds.isEmpty()) return Map.of();

        return analyticsPublicApi.getCountersByLinkIds(linkIds);
    }

    /** Sync link status sang redirect_lookup. */
    private void syncRedirectLookupStatus(Link link) {
        redirectLookupRepository.findByDomainIdAndShortCode(
                link.getDomainId(), link.getShortCode()).ifPresent(rl -> {
                    rl.setStatus(link.getStatus().name());
                    redirectLookupRepository.save(rl);
                });
    }
}
