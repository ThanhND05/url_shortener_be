package com.ThanhND05.url_shortener.analytics.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi chi tiết 1 click event.
 * Dùng trong API xem lịch sử click của link.
 */
@Builder
public record ClickEventResponse(
        UUID eventId,
        Instant occurredAt,
        String countryCode,
        String region,
        String city,
        String deviceType,
        String os,
        String browser,
        String refererDomain,
        boolean isBot
) {}
