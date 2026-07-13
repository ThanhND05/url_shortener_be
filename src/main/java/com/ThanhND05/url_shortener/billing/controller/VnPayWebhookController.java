package com.ThanhND05.url_shortener.billing.controller;

import com.ThanhND05.url_shortener.billing.service.BillingService;
import com.ThanhND05.url_shortener.common.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý callback từ VNPay — PHẢI PUBLIC (không yêu cầu JWT).
 *
 * VNPay gọi 2 endpoints:
 *
 * 1. IPN (Instant Payment Notification) — Server-to-Server:
 * GET
 * /api/v1/billing/vnpay-ipn?vnp_TxnRef=...&vnp_ResponseCode=...&vnp_SecureHash=...
 * → Xác nhận thanh toán thành công → Auto upgrade subscription.
 * → PHẢI trả về JSON {"RspCode": "00", "Message": "Confirm Success"}.
 *
 * 2. Return URL — User redirect về sau thanh toán:
 * GET /api/v1/billing/vnpay-return?vnp_TxnRef=...&vnp_ResponseCode=...
 * → Hiển thị kết quả cho user. KHÔNG cập nhật trạng thái (IPN đã làm).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class VnPayWebhookController {

    private final BillingService billingService;
    private final AppProperties appProperties;

    /**
     * IPN Endpoint — VNPay gọi Server-to-Server sau khi user thanh toán.
     * Đây là endpoint QUAN TRỌNG NHẤT để xác nhận giao dịch.
     *
     * VNPay yêu cầu response format:
     * {"RspCode": "00", "Message": "Confirm Success"}
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {
        // Thu thập tất cả params từ VNPay
        Map<String, String> params = extractVnPayParams(request);

        log.info("Received VNPay IPN callback: txnRef={}, responseCode={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        // Xử lý callback
        String rspCode = billingService.processIpnCallback(params);

        // Trả response cho VNPay
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", rspCode);
        response.put("Message", getIpnMessage(rspCode));

        return ResponseEntity.ok(response);
    }

    /**
     * Return URL — VNPay redirect user về sau thanh toán.
     * Chỉ dùng để hiển thị kết quả, trạng thái đã được IPN xử lý.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        Map<String, String> params = extractVnPayParams(request);

        boolean success = billingService.processReturnUrl(params);

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        // Determine frontend base URL dynamically from CORS configurations
        String allowedOrigins = appProperties.getCors().getAllowedOrigins();
        String frontendBase = "http://localhost:3000"; // default fallback
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            String[] origins = allowedOrigins.split(",");
            for (String origin : origins) {
                if (origin.trim().contains("3000")) {
                    frontendBase = origin.trim();
                    break;
                }
            }
            if (frontendBase.equals("http://localhost:3000") && origins.length > 0) {
                frontendBase = origins[origins.length - 1].trim();
            }
        }

        // Build redirect URL to frontend /payment-result route
        String redirectUrl = frontendBase + "/payment-result"
                + "?success=" + success
                + "&txnRef=" + txnRef
                + "&responseCode=" + responseCode;

        log.info("Redirecting user after VNPay payment: success={}, redirectUrl={}", success, redirectUrl);

        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .header(org.springframework.http.HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    // ── Private Helpers ─────────────────────────────────

    /**
     * Extract tất cả params có prefix "vnp_" từ request.
     */
    private Map<String, String> extractVnPayParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getKey().startsWith("vnp_") && entry.getValue().length > 0) {
                params.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return params;
    }

    private String getIpnMessage(String rspCode) {
        return switch (rspCode) {
            case "00" -> "Confirm Success";
            case "97" -> "Checksum Failed";
            case "02" -> "Order Not Found";
            case "01" -> "Order Already Confirmed";
            default -> "Unknown Error";
        };
    }
}
