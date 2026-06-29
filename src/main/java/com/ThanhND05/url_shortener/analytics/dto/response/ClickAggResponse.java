package com.ThanhND05.url_shortener.analytics.dto.response;

import lombok.Builder;

import java.time.Instant;

/**
 * DTO cho 1 điểm dữ liệu trong timeseries chart.
 * Dùng cho cả minute-level và daily-level aggregation.
 *
 * @param bucket      thời điểm bucket (đầu phút hoặc đầu ngày).
 * @param totalClicks tổng click trong bucket.
 * @param uniqueVisitors   unique visitors trong bucket.
 * @param botClicks   click từ bot/crawler.
 */
@Builder
public record ClickAggResponse(
        Instant bucket,
        long totalClicks,
        long uniqueVisitors,
        long botClicks,
        String countryCounts,
        String deviceCounts,
        String referrerCounts
) {}
