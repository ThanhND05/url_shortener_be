package com.ThanhND05.url_shortener.analytics.api.dto;

import java.time.Instant;

public record LinkCounterApiDto(
        Long linkId,
        long totalClicks,
        long uniqueVisitorsEstimate,
        Instant lastClickedAt
) {}
