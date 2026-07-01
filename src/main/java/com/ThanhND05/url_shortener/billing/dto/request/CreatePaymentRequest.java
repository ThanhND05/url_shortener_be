package com.ThanhND05.url_shortener.billing.dto.request;

/**
 * DTO yêu cầu tạo payment URL để nâng cấp lên gói Pro.
 *
 * @param ipAddress IP của client (dùng cho VNPay, controller tự điền).
 */
public record CreatePaymentRequest(
        String ipAddress
) {}
