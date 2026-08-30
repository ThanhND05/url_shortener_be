package com.ThanhND05.url_shortener.billing.service;

import com.ThanhND05.url_shortener.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý tích hợp VNPay:
 * - Tạo Payment URL (redirect user tới cổng VNPay)
 * - Verify checksum (xác thực IPN/Return từ VNPay)
 *
 * === Quy trình tạo Payment URL ===
 * 1. Tập hợp các tham số bắt buộc (vnp_TmnCode, vnp_Amount, vnp_TxnRef, ...).
 * 2. Sắp xếp tham số theo alphabet.
 * 3. Tạo query string từ các tham số.
 * 4. Hash query string bằng HmacSHA512 với vnp_HashSecret.
 * 5. Gắn vnp_SecureHash vào cuối URL → Payment URL hoàn chỉnh.
 *
 * === Quy trình Verify Checksum ===
 * 1. Tách vnp_SecureHash ra khỏi params.
 * 2. Tạo lại hash từ các params còn lại.
 * 3. So sánh hash → nếu khớp thì request hợp lệ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayService {

    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String VNP_CURR_CODE = "VND";
    private static final String VNP_LOCALE = "vn";
    private static final String VNP_ORDER_TYPE = "other";

    private static final DateTimeFormatter VNP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AppProperties appProperties;

    /**
     * Tạo Payment URL để redirect user tới cổng VNPay.
     *
     * @param txnRef    mã đơn hàng unique (gửi cho VNPay)
     * @param amount    số tiền VND (chưa nhân 100)
     * @param orderInfo mô tả đơn hàng
     * @param ipAddress IP của client
     * @return URL hoàn chỉnh để redirect
     */
    public String createPaymentUrl(String txnRef, long amount, String orderInfo, String ipAddress) {
        AppProperties.VnPay vnpayConfig = appProperties.getVnpay();

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String createDate = now.format(VNP_DATE_FORMAT);
        String expireDate = now.plusMinutes(15).format(VNP_DATE_FORMAT);

        // Tập hợp params (TreeMap tự sắp xếp theo alphabet)
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VNP_VERSION);
        params.put("vnp_Command", VNP_COMMAND);
        params.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu nhân 100
        params.put("vnp_CurrCode", VNP_CURR_CODE);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", VNP_ORDER_TYPE);
        params.put("vnp_Locale", VNP_LOCALE);
        params.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // Build query string (đã URL-encode)
        String queryString = buildQueryString(params);

        // Hash bằng HmacSHA512
        String secureHash = hmacSHA512(vnpayConfig.getHashSecret(), queryString);

        // Ghép URL hoàn chỉnh
        String paymentUrl = vnpayConfig.getPayUrl() + "?" + queryString
                + "&vnp_SecureHash=" + secureHash;

        log.info("Created VNPay payment URL for txnRef={}", txnRef);
        return paymentUrl;
    }

    /**
     * Verify checksum từ VNPay IPN/Return callback.
     *
     * @param params tất cả params nhận từ VNPay
     * @return true nếu checksum hợp lệ
     */
    public boolean verifyChecksum(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
            return false;
        }

        // Loại bỏ vnp_SecureHash và vnp_SecureHashType ra khỏi params
        Map<String, String> filteredParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().equals("vnp_SecureHash")
                    && !entry.getKey().equals("vnp_SecureHashType")
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                filteredParams.put(entry.getKey(), entry.getValue());
            }
        }

        String queryString = buildQueryString(filteredParams);
        String computedHash = hmacSHA512(appProperties.getVnpay().getHashSecret(), queryString);

        return computedHash.equalsIgnoreCase(vnpSecureHash);
    }

    /**
     * Tạo txnRef unique cho mỗi giao dịch.
     * Format: URLSHORT_{timestamp}_{random 4 chars}
     */
    public String generateTxnRef() {
        String timestamp = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "URLSHORT_" + timestamp + "_" + random;
    }

    // ── Private Helpers ─────────────────────────────────

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /**
     * HMAC-SHA512 hash — thuật toán VNPay yêu cầu để tạo chữ ký.
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo HMAC-SHA512", e);
        }
    }
}
