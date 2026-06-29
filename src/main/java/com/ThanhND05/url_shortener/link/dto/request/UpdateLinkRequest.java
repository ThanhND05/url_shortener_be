package com.ThanhND05.url_shortener.link.dto.request;

import java.time.Instant;
import java.util.Set;

/**
 * DTO cập nhật link — chỉ các trường non-null mới được cập nhật.
 */
public record UpdateLinkRequest(
        String originalUrl,
        String title,
        String description,
        String status,
        Short redirectType,
        Instant startsAt,
        Instant expiresAt,
        Long maxClicks,
        Set<Long> tagIds
) {}
