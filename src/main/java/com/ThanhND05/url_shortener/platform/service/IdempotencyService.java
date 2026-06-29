package com.ThanhND05.url_shortener.platform.service;

import com.ThanhND05.url_shortener.platform.entity.IdempotencyKey;
import com.ThanhND05.url_shortener.platform.entity.IdempotencyKeyId;
import com.ThanhND05.url_shortener.platform.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service quản lý idempotency keys — chống duplicate request.
 *
 * === FLOW SỬ DỤNG ===
 * 1. Client gửi: POST /api/v1/links  + Header: Idempotency-Key: abc-123
 * 2. Controller gọi: idempotencyService.check(userId, "abc-123", bodyHash)
 *    - Nếu key tồn tại + bodyHash khớp → trả cached response (409 hoặc original response).
 *    - Nếu key tồn tại + bodyHash KHÁC → trả lỗi "Request body mismatch".
 *    - Nếu key chưa tồn tại → trả empty → controller xử lý bình thường.
 * 3. Sau khi xử lý thành công: idempotencyService.save(userId, key, bodyHash, status, body)
 *
 * Key có TTL mặc định 24h → scheduled cleanup job xóa keys hết hạn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private static final long DEFAULT_TTL_HOURS = 24;

    /**
     * Check idempotency key đã tồn tại chưa.
     *
     * @return Optional.empty() nếu key mới, Optional(key) nếu đã xử lý trước đó.
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyKey> check(UUID ownerId, String key) {
        return idempotencyKeyRepository.findById(new IdempotencyKeyId(ownerId, key));
    }

    /**
     * Lưu idempotency key + cached response.
     * Gọi SAU KHI request đã xử lý thành công.
     */
    @Transactional
    public void save(UUID ownerId, String key, String requestHash,
                     int responseStatus, String responseBody) {
        IdempotencyKey entity = IdempotencyKey.builder()
                .ownerId(ownerId)
                .idempotencyKey(key)
                .requestHash(requestHash)
                .responseStatus(responseStatus)
                .responseBody(responseBody)
                .expiresAt(Instant.now().plus(DEFAULT_TTL_HOURS, ChronoUnit.HOURS))
                .build();
        idempotencyKeyRepository.save(entity);
    }

    /** Scheduled cleanup — xóa keys đã hết hạn (chạy mỗi giờ). */
    @Scheduled(fixedRate = 3600000)  // 1 giờ
    @Transactional
    public void cleanupExpiredKeys() {
        int deleted = idempotencyKeyRepository.deleteExpiredKeys(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency keys", deleted);
        }
    }
}
