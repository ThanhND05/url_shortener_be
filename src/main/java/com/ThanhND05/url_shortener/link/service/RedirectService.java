package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.common.exception.BusinessException;
import com.ThanhND05.url_shortener.common.exception.ResourceNotFoundException;
import com.ThanhND05.url_shortener.common.util.HashUtil;
import com.ThanhND05.url_shortener.link.entity.RedirectLookup;
import com.ThanhND05.url_shortener.link.event.LinkClickedEvent;
import com.ThanhND05.url_shortener.link.repository.RedirectLookupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service xử lý redirect — HOT PATH, cần tối ưu tốc độ tối đa.
 *
 * Flow redirect:
 * 1. Client GET /r/{shortCode}
 * 2. Service tìm redirect info (cache Redis → fallback DB).
 * 3. Validate: status ACTIVE, chưa hết hạn, chưa quá max_clicks, đã bắt đầu.
 * 4. Nếu link yêu cầu mật khẩu → trả 403 (client cần gửi password riêng).
 * 5. OK → trả original_url + redirect_type.
 * 6. Publish LinkClickedEvent (async) → Analytics module ghi nhận.
 *
 * Caching:
 * - @Cacheable("redirects") → key = shortCode.
 * - Khi link bị update/delete → LinkService evict cache tương ứng.
 * - TTL = 1 giờ (cấu hình trong RedisConfig, hiện dùng in-memory).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectService {

    private final RedirectLookupRepository redirectLookupRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tìm và validate redirect info.
     * Cache kết quả theo shortCode.
     *
     * @return URL gốc cần redirect tới.
     * @throws ResourceNotFoundException nếu short code không tồn tại.
     * @throws BusinessException nếu link không khả dụng (hết hạn, disabled, ...).
     */
    @Cacheable(value = "redirects", key = "#shortCode")
    public RedirectLookup resolve(String shortCode) {
        // Tìm bằng shortCode (first match, status ACTIVE)
        return redirectLookupRepository.findFirstByShortCodeAndStatus(shortCode, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Link", "shortCode", shortCode));
    }

    /**
     * Validate redirect lookup và trả URL đích.
     * Publish click event nếu thành công.
     */
    public String processRedirect(String shortCode, String clientIp,
                                  String userAgent, String referer) {
        RedirectLookup lookup = resolve(shortCode);

        // Check thời gian bắt đầu
        if (lookup.getStartsAt() != null && Instant.now().isBefore(lookup.getStartsAt())) {
            throw new BusinessException("Link chưa được kích hoạt.");
        }

        // Check hết hạn
        if (lookup.getExpiresAt() != null && Instant.now().isAfter(lookup.getExpiresAt())) {
            throw new BusinessException("Link đã hết hạn.");
        }

        // Check max clicks
        if (lookup.getMaxClicks() != null && lookup.getClickCount() >= lookup.getMaxClicks()) {
            throw new BusinessException("Link đã đạt giới hạn lượt truy cập.");
        }

        // Check password
        if (lookup.isPasswordRequired()) {
            throw new BusinessException("Link yêu cầu mật khẩu. Vui lòng cung cấp mật khẩu.");
        }

        // Publish click event (async — không block redirect response)
        eventPublisher.publishEvent(new LinkClickedEvent(
                lookup.getLinkId(), lookup.getLinkPublicId(),
                lookup.getDomainId(), lookup.getShortCode(),
                HashUtil.sha256Bytes(clientIp), userAgent, referer
        ));

        return lookup.getOriginalUrl();
    }

    /** Lấy redirect type (HTTP status code) cho redirect response. */
    public short getRedirectType(String shortCode) {
        RedirectLookup lookup = resolve(shortCode);
        return lookup.getRedirectType();
    }
}
