package com.ThanhND05.url_shortener.billing.dto.response;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Doanh thu theo ngày — dùng cho chart trên admin dashboard.
 */
@Builder
public record DailyRevenueResponse(
        LocalDate date,
        long revenue,
        long transactionCount
) {}
