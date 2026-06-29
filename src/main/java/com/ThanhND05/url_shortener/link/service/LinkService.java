package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.common.util.HashUtil;
import com.ThanhND05.url_shortener.link.dto.request.*;
import com.ThanhND05.url_shortener.link.dto.response.*;
import com.ThanhND05.url_shortener.link.entity.*;
import com.ThanhND05.url_shortener.link.enums.*;
import com.ThanhND05.url_shortener.link.event.LinkCreatedEvent;
import com.ThanhND05.url_shortener.link.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý short links — CRUD, sync redirect_lookup, quản lý rules.
 *
 * === FLOW TẠO LINK ===
 * 1. Resolve domain (custom hoặc default).
 * 2. Sinh short code (auto Base62 hoặc custom do user chọn).
 * 3. Hash original URL (SHA-256) — dùng để detect link trùng.
 * 4. Hash password nếu có (BCrypt).
 * 5. Lưu Link entity + gắn tags.
 * 6. Sync vào redirect_lookup (bảng denormalized cho hot path).
 * 7. Publish LinkCreatedEvent → audit log.
 *
 * === SYNC REDIRECT_LOOKUP ===
 * Mỗi khi INSERT/UPDATE link, service tự sync data sang redirect_lookup.
 * KHÔNG dùng DB trigger — app kiểm soát logic.
 * Khi update/delete → evict Redis cache (@CacheEvict).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final DomainRepository domainRepository;
    private final RedirectLookupRepository redirectLookupRepository;
    private final LinkRuleRepository linkRuleRepository;
    private final TagRepository tagRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // ── TẠO LINK ──────────────────────────────────────────

    @Transactional
    public LinkResponse createLink(UUID ownerId, CreateLinkRequest request) {
        // 1. Resolve domain
        Domain domain = resolveDomain(ownerId, request.domainId());

        // 2. Xác định short code
        String shortCode;
        ShortCodeType codeType;
        if (request.customCode() != null && !request.customCode().isBlank()) {
            // Custom code — validate format + check trùng
            shortCode = request.customCode().trim();
            if (!shortCode.matches("^[A-Za-z0-9_-]{3,64}$")) {
                throw new BusinessException("Short code chỉ chấp nhận A-Z, a-z, 0-9, _, - (3-64 ký tự).");
            }
            if (linkRepository.existsByDomainIdAndShortCode(domain.getId(), shortCode)) {
                throw new DuplicateResourceException("Link", "shortCode", shortCode);
            }
            codeType = ShortCodeType.CUSTOM;
        } else {
            // Auto-generate từ sequence + Base62
            shortCode = shortCodeGenerator.generate();
            codeType = ShortCodeType.GENERATED;
        }

        // 3. Build Link entity
        Link link = Link.builder()
                .ownerId(ownerId)
                .domainId(domain.getId())
                .shortCode(shortCode)
                .shortCodeType(codeType)
                .originalUrl(request.originalUrl())
                .originalUrlHash(HashUtil.sha256Hex(request.originalUrl()))
                .title(request.title())
                .description(request.description())
                .redirectType(request.redirectType() != null ? request.redirectType() : 302)
                .startsAt(request.startsAt())
                .expiresAt(request.expiresAt())
                .maxClicks(request.maxClicks())
                .build();

        // 4. Hash password nếu có
        if (request.password() != null && !request.password().isBlank()) {
            link.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        // 5. Gắn tags
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.tagIds()));
            link.setTags(tags);
        }

        link = linkRepository.save(link);

        // 6. Sync redirect_lookup
        syncRedirectLookup(link, domain);

        // 7. Publish event
        eventPublisher.publishEvent(new LinkCreatedEvent(
                link.getId(), link.getPublicId(), ownerId, shortCode));

        log.info("Link created: {} → {}", shortCode, request.originalUrl());
        return LinkResponse.from(link);
    }

    // ── XEM / DANH SÁCH ───────────────────────────────────

    @Transactional(readOnly = true)
    public LinkResponse getLink(UUID publicId) {
        Link link = findByPublicId(publicId);
        return LinkResponse.from(link);
    }

    @Transactional(readOnly = true)
    public Page<LinkResponse> listLinks(UUID ownerId, Pageable pageable) {
        return linkRepository.findByOwnerIdAndStatusNot(ownerId, LinkStatus.DELETED, pageable)
                .map(LinkResponse::from);
    }

    // ── CẬP NHẬT ──────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "redirects", key = "#result.shortCode()")
    public LinkResponse updateLink(UUID publicId, UpdateLinkRequest request, UUID ownerId) {
        Link link = findOwnedLink(publicId, ownerId);

        if (request.originalUrl() != null) {
            link.setOriginalUrl(request.originalUrl());
            link.setOriginalUrlHash(HashUtil.sha256Hex(request.originalUrl()));
        }
        if (request.title() != null)
            link.setTitle(request.title());
        if (request.description() != null)
            link.setDescription(request.description());
        if (request.status() != null)
            link.setStatus(LinkStatus.valueOf(request.status()));
        if (request.redirectType() != null)
            link.setRedirectType(request.redirectType());
        if (request.startsAt() != null)
            link.setStartsAt(request.startsAt());
        if (request.expiresAt() != null)
            link.setExpiresAt(request.expiresAt());
        if (request.maxClicks() != null)
            link.setMaxClicks(request.maxClicks());
        if (request.tagIds() != null) {
            link.setTags(new HashSet<>(tagRepository.findAllById(request.tagIds())));
        }

        link = linkRepository.save(link);

        // Re-sync redirect_lookup
        Domain domain = domainRepository.findById(link.getDomainId()).orElse(null);
        if (domain != null)
            syncRedirectLookup(link, domain);

        return LinkResponse.from(link);
    }

    // ── XÓA ───────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "redirects", key = "#shortCode")
    public void deleteLink(UUID publicId, UUID ownerId) {
        Link link = findOwnedLink(publicId, ownerId);
        link.setStatus(LinkStatus.DELETED);
        link.setDeletedAt(Instant.now());
        linkRepository.save(link);

        // Update redirect_lookup status → không redirect nữa
        redirectLookupRepository.findByDomainIdAndShortCode(link.getDomainId(), link.getShortCode())
                .ifPresent(rl -> {
                    rl.setStatus("DELETED");
                    redirectLookupRepository.save(rl);
                });
    }

    // ── LINK RULES ────────────────────────────────────────

    @Transactional
    public LinkRuleResponse addRule(UUID publicId, CreateLinkRuleRequest request, UUID ownerId) {
        Link link = findOwnedLink(publicId, ownerId);
        LinkRule rule = LinkRule.builder()
                .linkId(link.getId())
                .ruleType(RuleType.valueOf(request.ruleType()))
                .condition(request.condition())
                .targetUrl(request.targetUrl())
                .priority(request.priority() != null ? request.priority() : 100)
                .build();
        rule = linkRuleRepository.save(rule);
        return LinkRuleResponse.from(rule);
    }

    @Transactional(readOnly = true)
    public List<LinkRuleResponse> getRules(UUID publicId) {
        Link link = findByPublicId(publicId);
        return linkRuleRepository.findByLinkIdOrderByPriorityAsc(link.getId()).stream()
                .map(LinkRuleResponse::from).collect(Collectors.toList());
    }

    // ── HELPERS ───────────────────────────────────────────

    /** Resolve domain: custom (by publicId) hoặc default (by ownerId). */
    private Domain resolveDomain(UUID ownerId, UUID domainPublicId) {
        if (domainPublicId != null) {
            Domain domain = domainRepository.findByPublicId(domainPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Domain", "publicId", domainPublicId));
            if (domain.getStatus() != DomainStatus.ACTIVE) {
                throw new BusinessException("Domain chưa được xác minh hoặc đã bị chặn.");
            }
            return domain;
        }
        return domainRepository.findByOwnerIdAndIsDefaultTrue(ownerId)
                .orElseThrow(() -> new BusinessException("Bạn chưa có domain mặc định. Vui lòng tạo domain trước."));
    }

    /**
     * Sync data từ Link entity → redirect_lookup (denormalized table).
     * Gọi sau mỗi INSERT/UPDATE trên link.links.
     */
    private void syncRedirectLookup(Link link, Domain domain) {
        RedirectLookup rl = RedirectLookup.builder()
                .domainId(link.getDomainId())
                .shortCode(link.getShortCode())
                .linkId(link.getId())
                .linkPublicId(link.getPublicId())
                .originalUrl(link.getOriginalUrl())
                .status(link.getStatus().name())
                .redirectType(link.getRedirectType())
                .startsAt(link.getStartsAt())
                .expiresAt(link.getExpiresAt())
                .maxClicks(link.getMaxClicks())
                .clickCount(link.getClickCount())
                .passwordRequired(link.isPasswordProtected())
                .updatedAt(Instant.now())
                .build();
        redirectLookupRepository.save(rl);
    }

    private Link findByPublicId(UUID publicId) {
        return linkRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Link", "publicId", publicId));
    }

    private Link findOwnedLink(UUID publicId, UUID ownerId) {
        Link link = findByPublicId(publicId);
        if (!ownerId.equals(link.getOwnerId())) {
            throw new ResourceNotFoundException("Link", "publicId", publicId);
        }
        return link;
    }
}
