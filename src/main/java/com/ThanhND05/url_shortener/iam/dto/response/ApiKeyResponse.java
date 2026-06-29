package com.ThanhND05.url_shortener.iam.dto.response;

import com.ThanhND05.url_shortener.iam.entity.ApiKey;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi thông tin API key.
 * rawKey chỉ có giá trị lúc TẠO MỚI — sau đó luôn null (không thể xem lại).
 */
@Builder
public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        /** Raw key — CHỈ trả về khi vừa tạo, sau đó luôn null. */
        String rawKey,
        String[] scopes,
        int rateLimitPerMinute,
        String status,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant createdAt
) {
    public static ApiKeyResponse from(ApiKey key) {
        return from(key, null);
    }

    /** Khi tạo mới, truyền rawKey vào để trả cho client lần duy nhất. */
    public static ApiKeyResponse from(ApiKey key, String rawKey) {
        return ApiKeyResponse.builder()
                .id(key.getId())
                .name(key.getName())
                .keyPrefix(key.getKeyPrefix())
                .rawKey(rawKey)
                .scopes(key.getScopes())
                .rateLimitPerMinute(key.getRateLimitPerMinute())
                .status(key.getStatus().name())
                .expiresAt(key.getExpiresAt())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .build();
    }
}
