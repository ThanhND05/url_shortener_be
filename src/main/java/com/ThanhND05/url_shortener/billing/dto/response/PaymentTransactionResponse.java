package com.ThanhND05.url_shortener.billing.dto.response;

import com.ThanhND05.url_shortener.billing.entity.PaymentTransaction;
import lombok.Builder;

import java.time.Instant;

/**
 * Response chứa thông tin 1 giao dịch thanh toán.
 */
@Builder
public record PaymentTransactionResponse(
        String txnRef,
        long amount,
        String orderInfo,
        String status,
        String vnpResponseCode,
        String vnpBankCode,
        Instant createdAt
) {
    public static PaymentTransactionResponse from(PaymentTransaction tx) {
        return PaymentTransactionResponse.builder()
                .txnRef(tx.getTxnRef())
                .amount(tx.getAmount())
                .orderInfo(tx.getOrderInfo())
                .status(tx.getStatus().name())
                .vnpResponseCode(tx.getVnpResponseCode())
                .vnpBankCode(tx.getVnpBankCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
