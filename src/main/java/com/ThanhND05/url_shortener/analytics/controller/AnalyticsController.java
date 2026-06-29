package com.ThanhND05.url_shortener.analytics.controller;

import com.ThanhND05.url_shortener.analytics.dto.response.*;
import com.ThanhND05.url_shortener.analytics.service.AnalyticsQueryService;
import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Controller analytics — cung cấp API thống kê cho dashboard.
 *
 *   GET /api/v1/analytics/links/{publicId}/stats       → tổng hợp counters + timeseries 24h.
 *   GET /api/v1/analytics/links/{publicId}/timeseries   → timeseries custom range.
 *   GET /api/v1/analytics/links/{publicId}/clicks       → raw click log (phân trang).
 *
 * Tất cả endpoint yêu cầu permission "analytics:read".
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('analytics:read')")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    /**
     * Lấy stats tổng hợp cho 1 link: counters + timeseries mặc định 24h.
     *
     * Response bao gồm:
     * - totalClicks, uniqueVisitorsEstimate, lastClickedAt (từ link_counters).
     * - timeseries: danh sách data points theo phút trong 24h qua.
     */
    @GetMapping("/links/{publicId}/stats")
    public ResponseEntity<ApiResponse<LinkStatsResponse>> getStats(
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsQueryService.getStats(publicId)));
    }

    /**
     * Lấy timeseries cho 1 link trong khoảng thời gian tùy chỉnh.
     *
     * Granularity tự động:
     * - Range ≤ 24h → dữ liệu theo phút (từ click_agg_minute).
     * - Range > 24h → dữ liệu theo ngày (từ click_agg_daily).
     *
     * @param from thời điểm bắt đầu (ISO-8601, VD: 2026-06-01T00:00:00Z).
     * @param to   thời điểm kết thúc (null = now).
     */
    @GetMapping("/links/{publicId}/timeseries")
    public ResponseEntity<ApiResponse<List<ClickAggResponse>>> getTimeseries(
            @PathVariable UUID publicId,
            @RequestParam Instant from,
            @RequestParam(required = false) Instant to) {
        if (to == null) to = Instant.now();
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsQueryService.getTimeseries(publicId, from, to)));
    }

    /**
     * Lấy raw click log cho 1 link (phân trang).
     * Dùng cho bảng "Recent Clicks" hiển thị chi tiết từng lượt truy cập.
     *
     * @param from thời điểm bắt đầu (null = 7 ngày trước).
     * @param to   thời điểm kết thúc (null = now).
     */
    @GetMapping("/links/{publicId}/clicks")
    public ResponseEntity<ApiResponse<PageResponse<ClickEventResponse>>> getClickLog(
            @PathVariable UUID publicId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        if (from == null) from = Instant.now().minus(7, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(analyticsQueryService.getClickLog(publicId, from, to, pageable))));
    }
}
