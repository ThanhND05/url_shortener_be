package com.ThanhND05.url_shortener.analytics.controller;

import com.ThanhND05.url_shortener.analytics.dto.response.*;
import com.ThanhND05.url_shortener.analytics.service.AdminAnalyticsService;
import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Controller admin analytics — cung cấp API dashboard tổng quan hệ thống.
 *
 * Endpoints:
 *   GET /api/v1/admin/analytics/overview    → tổng links, clicks, users, top links.
 *   GET /api/v1/admin/analytics/top-links   → top N links theo click count.
 *   GET /api/v1/admin/analytics/breakdown   → breakdown devices/OS/browser/country/referrer.
 *   GET /api/v1/admin/analytics/timeseries  → timeseries click toàn hệ thống theo ngày.
 *
 * Tất cả endpoint yêu cầu permission "user:manage" (Super Admin / Admin).
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    /**
     * Tổng quan hệ thống: counts, period stats, status breakdown, top links.
     *
     * Response cung cấp mọi metric cần thiết để render admin dashboard overview page:
     * - Lifetime totals (totalLinks, totalClicks, totalUsers).
     * - Period breakdowns (today, 7d, 30d) cho links, clicks, users.
     * - Link status breakdown (active, disabled, quarantined, expired).
     * - Top 10 links.
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.ok(adminAnalyticsService.getOverview()));
    }

    /**
     * Top N links có nhiều click nhất (real-time từ link_counters).
     *
     * @param limit số lượng links trả về (mặc định 10, tối đa 100).
     */
    @GetMapping("/top-links")
    public ResponseEntity<ApiResponse<List<TopLinkResponse>>> getTopLinks(
            @RequestParam(defaultValue = "10") int limit) {
        if (limit < 1) limit = 10;
        if (limit > 100) limit = 100;
        return ResponseEntity.ok(ApiResponse.ok(adminAnalyticsService.getTopLinks(limit)));
    }

    /**
     * Breakdown phân tích clicks theo nhiều chiều.
     *
     * @param from thời điểm bắt đầu (ISO-8601). Mặc định: 30 ngày trước.
     * @param to   thời điểm kết thúc (ISO-8601). Mặc định: now.
     * @return BreakdownResponse chứa Map<String, Long> cho mỗi chiều.
     */
    @GetMapping("/breakdown")
    public ResponseEntity<ApiResponse<BreakdownResponse>> getBreakdown(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();
        return ResponseEntity.ok(ApiResponse.ok(adminAnalyticsService.getBreakdown(from, to)));
    }

    /**
     * Timeseries tổng click toàn hệ thống theo ngày.
     * Dùng cho line/bar chart trên admin dashboard.
     *
     * @param days số ngày lấy dữ liệu (mặc định 30, tối đa 365).
     * @return List<ClickAggResponse> mỗi ngày 1 data point.
     */
    @GetMapping("/timeseries")
    public ResponseEntity<ApiResponse<List<ClickAggResponse>>> getTimeseries(
            @RequestParam(defaultValue = "30") int days) {
        if (days < 1) days = 30;
        if (days > 365) days = 365;

        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(days);

        return ResponseEntity.ok(ApiResponse.ok(
                adminAnalyticsService.getSystemTimeseries(from, to)));
    }
}
