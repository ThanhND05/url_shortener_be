package com.ThanhND05.url_shortener.billing.controller;

import com.ThanhND05.url_shortener.billing.dto.response.AdminBillingOverviewResponse;
import com.ThanhND05.url_shortener.billing.dto.response.AdminPaymentTransactionResponse;
import com.ThanhND05.url_shortener.billing.dto.response.DailyRevenueResponse;
import com.ThanhND05.url_shortener.billing.enums.PaymentStatus;
import com.ThanhND05.url_shortener.billing.service.AdminBillingService;
import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller admin billing — cung cấp API quản lý tài chính cho admin dashboard.
 *
 * Endpoints:
 *   GET /api/v1/admin/billing/overview        → Tổng quan doanh thu (tổng, theo period, subscription stats).
 *   GET /api/v1/admin/billing/transactions    → Danh sách giao dịch (filter status/userId, phân trang).
 *   GET /api/v1/admin/billing/revenue-chart   → Revenue timeseries theo ngày (cho chart).
 *
 * Tất cả endpoint yêu cầu permission "user:manage" (Super Admin / Admin).
 */
@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class AdminBillingController {

    private final AdminBillingService adminBillingService;

    /**
     * Tổng quan doanh thu toàn hệ thống.
     *
     * Response bao gồm:
     * - Tổng doanh thu all-time, hôm nay, 7 ngày, 30 ngày.
     * - Đếm giao dịch theo status (SUCCESS, FAILED, PENDING).
     * - Đếm users theo gói (Pro, Free).
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminBillingOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.ok(adminBillingService.getOverview()));
    }

    /**
     * Danh sách giao dịch thanh toán toàn hệ thống.
     * Hỗ trợ filter theo status và userId.
     *
     * @param status filter theo trạng thái (PENDING, SUCCESS, FAILED). Nullable.
     * @param userId filter theo user cụ thể. Nullable.
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<AdminPaymentTransactionResponse>>> listTransactions(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(adminBillingService.listTransactions(status, userId, pageable))));
    }

    /**
     * Revenue timeseries theo ngày — dùng cho chart trên admin dashboard.
     *
     * @param days số ngày lấy dữ liệu (mặc định 30, tối đa 365).
     */
    @GetMapping("/revenue-chart")
    public ResponseEntity<ApiResponse<List<DailyRevenueResponse>>> getRevenueChart(
            @RequestParam(defaultValue = "30") int days) {
        if (days < 1) days = 30;
        if (days > 365) days = 365;
        return ResponseEntity.ok(ApiResponse.ok(adminBillingService.getRevenueTimeseries(days)));
    }
}
