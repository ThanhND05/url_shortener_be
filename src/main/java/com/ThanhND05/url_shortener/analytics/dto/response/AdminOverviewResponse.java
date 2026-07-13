package com.ThanhND05.url_shortener.analytics.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * DTO tổng quan hệ thống dành cho admin dashboard.
 *
 * Bao gồm:
 * - Tổng links, tổng clicks, tổng users.
 * - Số liệu mới trong hôm nay / 7 ngày / 30 ngày (links, users, clicks).
 * - Top N links có lượt click cao nhất.
 */
@Builder
public record AdminOverviewResponse(

        // ── Lifetime totals ──
        long totalLinks,
        long totalClicks,
        long totalUsers,

        // ── Links created ──
        long linksToday,
        long linksLast7Days,
        long linksLast30Days,

        // ── Clicks ──
        long clicksToday,
        long clicksLast7Days,
        long clicksLast30Days,

        // ── Users registered ──
        long usersToday,
        long usersLast7Days,
        long usersLast30Days,

        // ── Status breakdown ──
        long activeLinks,
        long disabledLinks,
        long quarantinedLinks,
        long expiredLinks,

        // ── Top links ──
        List<TopLinkResponse> topLinks
) {}
