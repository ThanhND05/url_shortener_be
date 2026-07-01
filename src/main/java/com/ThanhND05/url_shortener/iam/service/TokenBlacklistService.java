package com.ThanhND05.url_shortener.iam.service;

import com.ThanhND05.url_shortener.iam.entity.TokenBlacklist;
import com.ThanhND05.url_shortener.iam.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service quản lý Access Token Blacklist.
 *
 * === Chiến lược lưu trữ: Redis + DB (Write-Through) ===
 *
 * Khi blacklist 1 token:
 *   1. Lưu vào Redis với TTL = thời gian sống còn lại của token → truy vấn cực nhanh (<1ms).
 *   2. Lưu vào PostgreSQL (bảng iam.token_blacklist) → backup phòng khi Redis restart.
 *
 * Khi kiểm tra token có bị blacklist:
 *   1. Check Redis trước (hasKey) — tốc độ O(1).
 *   2. Nếu Redis miss (VD: sau restart), fallback check DB → nếu có thì warm lại Redis.
 *
 * Cleanup:
 *   - Redis: tự động xóa nhờ TTL, không cần cleanup job.
 *   - DB: Scheduled job chạy mỗi giờ, xóa các entry đã hết hạn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    /** Redis key prefix cho blacklisted access token JTI. */
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Blacklist một access token.
     *
     * @param jti       JWT ID (claim "jti") của access token
     * @param userId    ID của user sở hữu token
     * @param expiresAt thời điểm access token hết hạn
     * @param reason    lý do blacklist (LOGOUT, LOGOUT_ALL, PASSWORD_CHANGE, ...)
     */
    @Transactional
    public void blacklist(UUID jti, UUID userId, Instant expiresAt, String reason) {
        // Tính TTL = thời gian còn lại cho đến khi token hết hạn tự nhiên
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            // Token đã hết hạn rồi → không cần blacklist
            log.debug("Token {} đã hết hạn, bỏ qua blacklist.", jti);
            return;
        }

        // 1. Lưu vào Redis với TTL
        String redisKey = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(redisKey, reason, ttl);
        log.debug("Blacklisted token {} in Redis (TTL: {}s)", jti, ttl.getSeconds());

        // 2. Lưu vào DB (backup)
        TokenBlacklist entry = TokenBlacklist.builder()
                .jti(jti)
                .userId(userId)
                .expiresAt(expiresAt)
                .reason(reason)
                .build();
        tokenBlacklistRepository.save(entry);
        log.debug("Blacklisted token {} in DB", jti);
    }

    /**
     * Kiểm tra JTI có bị blacklist hay không.
     * Ưu tiên check Redis (nhanh), fallback DB nếu Redis miss.
     *
     * @param jti JWT ID cần kiểm tra
     * @return true nếu token đã bị blacklist
     */
    public boolean isBlacklisted(String jti) {
        String redisKey = BLACKLIST_PREFIX + jti;

        // 1. Check Redis trước
        Boolean existsInRedis = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(existsInRedis)) {
            return true;
        }

        // 2. Fallback: check DB (trường hợp Redis bị restart mất data)
        UUID jtiUuid;
        try {
            jtiUuid = UUID.fromString(jti);
        } catch (IllegalArgumentException e) {
            // JTI không phải UUID hợp lệ → chắc chắn không nằm trong blacklist
            return false;
        }

        boolean existsInDb = tokenBlacklistRepository.existsByJti(jtiUuid);
        if (existsInDb) {
            // Warm lại Redis để lần sau không cần query DB
            tokenBlacklistRepository.findById(jtiUuid).ifPresent(entry -> {
                Duration ttl = Duration.between(Instant.now(), entry.getExpiresAt());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(redisKey, entry.getReason(), ttl);
                    log.debug("Warmed Redis cache for blacklisted token {}", jti);
                }
            });
            return true;
        }

        return false;
    }

    /**
     * Cleanup job: xóa các entry đã hết hạn trong DB.
     * Redis tự cleanup nhờ TTL, nhưng DB cần job riêng.
     * Chạy mỗi giờ.
     */
    @Scheduled(fixedRate = 3600_000) // 1 giờ
    @Transactional
    public void cleanupExpiredEntries() {
        int deleted = tokenBlacklistRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired token blacklist entries.", deleted);
        }
    }
}
