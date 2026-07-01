package com.ThanhND05.url_shortener.billing.controller;

import com.ThanhND05.url_shortener.billing.dto.response.PaymentTransactionResponse;
import com.ThanhND05.url_shortener.billing.dto.response.PaymentUrlResponse;
import com.ThanhND05.url_shortener.billing.dto.response.SubscriptionResponse;
import com.ThanhND05.url_shortener.billing.service.BillingService;
import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý billing & subscription.
 *
 * Endpoints (yêu cầu authenticated):
 *   GET  /api/v1/billing/subscription       → Xem gói hiện tại
 *   POST /api/v1/billing/create-payment      → Tạo payment URL nâng cấp Pro
 *   GET  /api/v1/billing/payment-history     → Xem lịch sử giao dịch
 *
 * VNPay callback endpoints (public — VNPay gọi):
 *   → Xem VnPayWebhookController
 */
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /**
     * Xem gói dịch vụ hiện tại của user.
     * Response bao gồm: plan, status, isPro, links đã dùng, giới hạn.
     */
    @GetMapping("/subscription")
    @PreAuthorize("hasAuthority('billing:read')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription() {
        SubscriptionResponse response = billingService.getCurrentSubscription(
                SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Tạo payment URL VNPay để nâng cấp lên gói Pro (50.000đ/tháng).
     * Client nhận URL → redirect user tới VNPay → user quét QR thanh toán.
     */
    @PostMapping("/create-payment")
    @PreAuthorize("hasAuthority('billing:manage')")
    public ResponseEntity<ApiResponse<PaymentUrlResponse>> createPayment(
            HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        PaymentUrlResponse response = billingService.createProUpgradePayment(
                SecurityUtils.getCurrentUserId(), ipAddress);
        return ResponseEntity.ok(ApiResponse.ok(response, "Vui lòng thanh toán qua VNPay."));
    }

    /**
     * Xem lịch sử giao dịch thanh toán.
     */
    @GetMapping("/payment-history")
    @PreAuthorize("hasAuthority('billing:read')")
    public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getPaymentHistory() {
        List<PaymentTransactionResponse> history = billingService.getPaymentHistory(
                SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    // ── Helpers ─────────────────────────────────────────

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
