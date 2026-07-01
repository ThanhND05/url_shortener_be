package com.ThanhND05.url_shortener.billing.dto.response;

import lombok.Builder;

/**
 * Response chứa URL thanh toán VNPay để client redirect.
 */
@Builder
public record PaymentUrlResponse(
        String paymentUrl,
        String txnRef
) {}
