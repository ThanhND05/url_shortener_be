package com.ThanhND05.url_shortener.analytics.dto.response;

import lombok.Builder;

import java.util.Map;

/**
 * DTO breakdown phân tích clicks theo các chiều: device, OS, browser, country, referrer.
 *
 * Mỗi field là Map<String, Long> — key là giá trị phân loại, value là số lượt click.
 * VD: devices = {"mobile": 1200, "desktop": 800, "tablet": 150}
 *
 * Dùng trong endpoint GET /api/v1/admin/analytics/breakdown
 */
@Builder
public record BreakdownResponse(
        Map<String, Long> devices,
        Map<String, Long> operatingSystems,
        Map<String, Long> browsers,
        Map<String, Long> countries,
        Map<String, Long> referrers
) {}
