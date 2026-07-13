package com.ThanhND05.url_shortener.analytics.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO cho top links — kết hợp thông tin link + click count real-time.
 *
 * Dùng trong endpoint GET /api/v1/admin/analytics/top-links
 * và trong AdminOverviewResponse.topLinks.
 */
@Builder
public record TopLinkResponse(
        UUID publicId,
        String shortCode,
        String originalUrl,
        String title,
        String ownerEmail,
        String status,
        short redirectType,
        long totalClicks,
        long uniqueVisitors,
        Instant lastClickedAt,
        Set<String> tags,
        Instant createdAt
) {}
