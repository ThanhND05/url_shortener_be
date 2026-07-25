package com.ThanhND05.url_shortener.billing.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Response mở rộng cho admin — bao gồm userId & email chủ giao dịch.
 */
@Builder
public record AdminPaymentTransactionResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String txnRef,
        long amount,
        String orderInfo,
        String status,
        String vnpResponseCode,
        String vnpBankCode,
        String vnpPayDate,
        Instant createdAt,
        Instant updatedAt
) {}
