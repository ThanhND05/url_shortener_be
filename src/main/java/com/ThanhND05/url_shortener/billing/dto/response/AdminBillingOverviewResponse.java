package com.ThanhND05.url_shortener.billing.dto.response;

import lombok.Builder;

/**
 * Response tổng quan doanh thu cho admin dashboard.
 */
@Builder
public record AdminBillingOverviewResponse(
        /** Tổng doanh thu (VND) từ trước đến nay (chỉ giao dịch SUCCESS). */
        long totalRevenue,

        /** Tổng số giao dịch thành công. */
        long totalSuccessTransactions,

        /** Tổng số giao dịch thất bại. */
        long totalFailedTransactions,

        /** Tổng số giao dịch đang chờ. */
        long totalPendingTransactions,

        /** Doanh thu hôm nay (VND). */
        long revenueToday,

        /** Doanh thu 7 ngày gần nhất (VND). */
        long revenue7Days,

        /** Doanh thu 30 ngày gần nhất (VND). */
        long revenue30Days,

        /** Tổng user đang dùng gói Pro. */
        long totalProUsers,

        /** Tổng user dùng gói Free. */
        long totalFreeUsers
) {}
