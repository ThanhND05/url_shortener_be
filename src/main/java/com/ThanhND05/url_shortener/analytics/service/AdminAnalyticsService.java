package com.ThanhND05.url_shortener.analytics.service;

import com.ThanhND05.url_shortener.analytics.dto.response.*;
import com.ThanhND05.url_shortener.analytics.entity.LinkCounter;
import com.ThanhND05.url_shortener.analytics.repository.*;
import com.ThanhND05.url_shortener.iam.api.IamPublicApi;
import com.ThanhND05.url_shortener.link.api.LinkPublicApi;
import com.ThanhND05.url_shortener.link.api.dto.LinkApiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service tổng hợp analytics toàn hệ thống dành cho admin dashboard.
 *
 * Chức năng:
 * - Overview: tổng links, clicks, users + so sánh theo khoảng thời gian.
 * - Top Links: N links có lượt click cao nhất (từ link_counters).
 * - Breakdown: phân tích theo device/OS/browser/country/referrer.
 * - Timeseries: click trend toàn hệ thống theo ngày.
 *
 * Tối ưu production:
 * - Batch-load links + users để tránh N+1 trong getTopLinks().
 * - Cache overview (TTL 2 phút) để tránh bắn 15+ queries mỗi lần load dashboard.
 * - Cache breakdown (TTL 5 phút) vì aggregate trên click_events rất nặng.
 * - Timeseries đọc từ click_agg_daily (pre-aggregated) → nhẹ, không cần cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsService {

    private final LinkPublicApi linkPublicApi;
    private final IamPublicApi iamPublicApi;
    private final LinkCounterRepository linkCounterRepository;
    private final ClickEventRepository clickEventRepository;
    private final ClickAggDailyRepository clickAggDailyRepository;

    // ── OVERVIEW ─────────────────────────────────────────

    /**
     * Tổng hợp overview toàn hệ thống.
     *
     * Cached 2 phút — dashboard admin không cần real-time,
     * giảm từ ~37 queries xuống 0 cho requests trong cùng window.
     */
    @Cacheable(value = "admin:overview", key = "'system'")
    public AdminOverviewResponse getOverview() {
        Instant now = Instant.now();
        Instant startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        Instant last7Days = now.minus(7, ChronoUnit.DAYS);
        Instant last30Days = now.minus(30, ChronoUnit.DAYS);

        // Lifetime totals
        long totalLinks = linkPublicApi.countLinksByStatusNotDeleted();
        long totalClicks = linkCounterRepository.sumTotalClicks();
        long totalUsers = iamPublicApi.countTotalUsers();

        // Links created in periods
        long linksToday = linkPublicApi.countLinksCreatedBetween(startOfToday, now);
        long linksLast7Days = linkPublicApi.countLinksCreatedBetween(last7Days, now);
        long linksLast30Days = linkPublicApi.countLinksCreatedBetween(last30Days, now);

        // Clicks in periods
        long clicksToday = clickEventRepository.countByOccurredAtBetween(startOfToday, now);
        long clicksLast7Days = clickEventRepository.countByOccurredAtBetween(last7Days, now);
        long clicksLast30Days = clickEventRepository.countByOccurredAtBetween(last30Days, now);

        // Users registered in periods
        long usersToday = iamPublicApi.countUsersCreatedAfter(startOfToday);
        long usersLast7Days = iamPublicApi.countUsersCreatedAfter(last7Days);
        long usersLast30Days = iamPublicApi.countUsersCreatedAfter(last30Days);

        // Link status breakdown
        long activeLinks = linkPublicApi.countLinksByStatus("ACTIVE");
        long disabledLinks = linkPublicApi.countLinksByStatus("DISABLED");
        long quarantinedLinks = linkPublicApi.countLinksByStatus("QUARANTINED");
        long expiredLinks = linkPublicApi.countLinksByStatus("EXPIRED");

        // Top 10 links (đã tối ưu batch-load bên trong)
        List<TopLinkResponse> topLinks = getTopLinksInternal(10);

        return AdminOverviewResponse.builder()
                .totalLinks(totalLinks)
                .totalClicks(totalClicks)
                .totalUsers(totalUsers)
                .linksToday(linksToday)
                .linksLast7Days(linksLast7Days)
                .linksLast30Days(linksLast30Days)
                .clicksToday(clicksToday)
                .clicksLast7Days(clicksLast7Days)
                .clicksLast30Days(clicksLast30Days)
                .usersToday(usersToday)
                .usersLast7Days(usersLast7Days)
                .usersLast30Days(usersLast30Days)
                .activeLinks(activeLinks)
                .disabledLinks(disabledLinks)
                .quarantinedLinks(quarantinedLinks)
                .expiredLinks(expiredLinks)
                .topLinks(topLinks)
                .build();
    }

    // ── TOP LINKS ────────────────────────────────────────

    /**
     * Lấy top N links — public endpoint (có thể gọi riêng).
     */
    public List<TopLinkResponse> getTopLinks(int limit) {
        return getTopLinksInternal(limit);
    }

    /**
     * Internal: batch-load links + users để tránh N+1.
     *
     * Flow tối ưu (3 queries thay vì 1 + N×2):
     * 1. Query link_counters ORDER BY total_clicks DESC LIMIT N → 1 query.
     * 2. Batch-load tất cả Link entities bằng findAllById() → 1 query.
     * 3. Batch-load tất cả User entities bằng findAllById() → 1 query.
     * 4. Map kết quả in-memory.
     */
    private List<TopLinkResponse> getTopLinksInternal(int limit) {
        List<LinkCounter> topCounters = linkCounterRepository
                .findTopByOrderByTotalClicksDesc(PageRequest.of(0, limit));

        if (topCounters.isEmpty()) return List.of();

        // Batch-load links (1 query)
        List<Long> linkIds = topCounters.stream()
                .map(LinkCounter::getLinkId)
                .toList();
        Map<Long, LinkApiDto> linkMap = linkPublicApi.getLinksByIds(linkIds);

        // Batch-load users (1 query)
        Set<UUID> ownerIds = linkMap.values().stream()
                .map(LinkApiDto::ownerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> ownerEmailMap = iamPublicApi.getUserEmails(ownerIds);

        // Map in-memory
        return topCounters.stream()
                .map(counter -> {
                    LinkApiDto link = linkMap.get(counter.getLinkId());
                    if (link == null || "DELETED".equals(link.status())) return null;

                    String ownerEmail = link.ownerId() != null
                            ? ownerEmailMap.get(link.ownerId())
                            : null;

                    Set<String> tagNames = link.tags();

                    return TopLinkResponse.builder()
                            .publicId(link.publicId())
                            .shortCode(link.shortCode())
                            .originalUrl(link.originalUrl())
                            .title(link.title())
                            .ownerEmail(ownerEmail)
                            .status(link.status())
                            .redirectType(link.redirectType())
                            .totalClicks(counter.getTotalClicks())
                            .uniqueVisitors(counter.getUniqueVisitorsEstimate())
                            .lastClickedAt(counter.getLastClickedAt())
                            .tags(tagNames)
                            .createdAt(link.createdAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ── BREAKDOWN ────────────────────────────────────────

    /**
     * Phân tích clicks theo nhiều chiều trong khoảng thời gian.
     *
     * Cached 5 phút — 5 aggregate queries trên bảng click_events (hàng triệu rows)
     * rất nặng, kết quả không cần real-time cho admin dashboard.
     */
    @Cacheable(value = "admin:breakdown", key = "#from.toString() + '_' + #to.toString()")
    public BreakdownResponse getBreakdown(Instant from, Instant to) {
        Map<String, Long> devices = toMap(clickEventRepository.countByDeviceType(from, to));
        Map<String, Long> os = toMap(clickEventRepository.countByOs(from, to));
        Map<String, Long> browsers = toMap(clickEventRepository.countByBrowser(from, to));
        Map<String, Long> countries = toMap(clickEventRepository.countByCountry(from, to));
        Map<String, Long> referrers = toMap(clickEventRepository.countByReferrer(from, to));

        return BreakdownResponse.builder()
                .devices(devices)
                .operatingSystems(os)
                .browsers(browsers)
                .countries(countries)
                .referrers(referrers)
                .build();
    }

    // ── TIMESERIES ───────────────────────────────────────

    /**
     * Timeseries tổng click toàn hệ thống theo ngày.
     * Đọc từ click_agg_daily (pre-aggregated) → nhẹ, không cần cache thêm.
     *
     * @param from ngày bắt đầu.
     * @param to   ngày kết thúc.
     * @return List<ClickAggResponse> mỗi ngày 1 data point.
     */
    public List<ClickAggResponse> getSystemTimeseries(LocalDate from, LocalDate to) {
        List<Object[]> rows = clickAggDailyRepository.systemWideDailyTimeseries(from, to);

        return rows.stream()
                .map(row -> ClickAggResponse.builder()
                        .bucket(((LocalDate) row[0]).atStartOfDay(ZoneOffset.UTC).toInstant())
                        .totalClicks((Long) row[1])
                        .uniqueVisitors((Long) row[2])
                        .botClicks((Long) row[3])
                        .build())
                .toList();
    }

    // ── HELPERS ──────────────────────────────────────────

    /** Convert List<Object[]> (key, count) → LinkedHashMap (preserves DESC order). */
    private Map<String, Long> toMap(List<Object[]> rows) {
        LinkedHashMap<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "unknown";
            Long count = (Long) row[1];
            map.put(key, count);
        }
        return map;
    }
}
