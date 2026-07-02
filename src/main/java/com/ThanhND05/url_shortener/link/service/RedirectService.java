package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.analytics.dto.ClickEventMessage;
import com.ThanhND05.url_shortener.analytics.kafka.ClickEventProducer;
import com.ThanhND05.url_shortener.common.exception.BusinessException;
import com.ThanhND05.url_shortener.common.exception.ResourceNotFoundException;
import com.ThanhND05.url_shortener.common.util.HashUtil;
import com.ThanhND05.url_shortener.link.entity.RedirectLookup;
import com.ThanhND05.url_shortener.link.repository.RedirectLookupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
 * 6. Bắn click event vào Kafka (non-blocking, < 1ms) → Consumer batch INSERT DB.
 *
 * === THAY ĐỔI QUAN TRỌNG ===
 * TRƯỚC: eventPublisher.publishEvent(LinkClickedEvent) → @Async INSERT 1-by-1.
 * SAU:   clickEventProducer.send(ClickEventMessage) → Kafka → batch INSERT 500 rows/lần.
 *
 * Lý do thay đổi:
 * - ApplicationEventPublisher lưu event trong RAM → server crash = mất event.
 * - Kafka lưu message trên disk (replicated) → không bao giờ mất.
 * - Batch insert 500 rows giảm 500x DB round-trip.
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
    private final ClickEventProducer clickEventProducer;

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
     * Bắn click event vào Kafka nếu thành công.
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

        // ── BẮN CLICK EVENT VÀO KAFKA (thay vì ApplicationEventPublisher) ──
        // Non-blocking, < 1ms — message được lưu bền vững trên Kafka disk.
        // ClickEventConsumer sẽ gom 500 messages → batch INSERT DB 1 lần.
        ClickEventMessage message = ClickEventMessage.from(
                lookup.getLinkId(), lookup.getLinkPublicId(),
                lookup.getDomainId(), lookup.getShortCode(),
                HashUtil.sha256Bytes(clientIp), userAgent, referer
        );
        clickEventProducer.send(message);

        return lookup.getOriginalUrl();
    }

    /** Lấy redirect type (HTTP status code) cho redirect response. */
    public short getRedirectType(String shortCode) {
        RedirectLookup lookup = resolve(shortCode);
        return lookup.getRedirectType();
    }
}
