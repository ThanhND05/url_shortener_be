package com.ThanhND05.url_shortener.analytics.service;

import com.ThanhND05.url_shortener.analytics.entity.ClickAggDaily;
import com.ThanhND05.url_shortener.analytics.entity.ClickAggMinute;
import com.ThanhND05.url_shortener.analytics.entity.ClickEvent;
import com.ThanhND05.url_shortener.analytics.repository.ClickAggDailyRepository;
import com.ThanhND05.url_shortener.analytics.repository.ClickAggMinuteRepository;
import com.ThanhND05.url_shortener.analytics.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service chạy scheduled jobs để gộp (aggregate) dữ liệu analytics.
 *
 * === PIPELINE AGGREGATION ===
 *
 *  click_events (raw)         ──[mỗi phút]──▶  click_agg_minute
 *  click_agg_minute           ──[mỗi ngày]──▶  click_agg_daily
 *
 * Tại sao dùng 2 tầng aggregation?
 * 1. click_agg_minute: real-time dashboard (chart last 1h/6h/24h), giữ 7 ngày.
 * 2. click_agg_daily: long-term analytics (chart 30d/90d/365d), giữ vĩnh viễn.
 * → Query nhanh vì data đã pre-computed, không cần scan hàng triệu raw events.
 *
 * Scheduled timing:
 * - minuteAggregation: chạy mỗi 60 giây (fixedRate), xử lý phút trước.
 * - dailyAggregation:  chạy lúc 00:05 hàng ngày, roll-up ngày hôm trước.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final ClickEventRepository clickEventRepository;
    private final ClickAggMinuteRepository clickAggMinuteRepository;
    private final ClickAggDailyRepository clickAggDailyRepository;

    /**
     * Job chạy mỗi 60 giây — gộp click_events của phút trước thành click_agg_minute.
     *
     * Logic:
     * 1. Xác định time window: [phút trước bắt đầu, phút trước kết thúc).
     * 2. Query raw click_events trong window.
     * 3. Group by link_id → tính total, unique, bot, country/device/referrer counts.
     * 4. UPSERT vào click_agg_minute.
     */
    @Scheduled(fixedRate = 60000)  // 60 giây
    @Transactional
    public void minuteAggregation() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant bucketStart = now.minus(1, ChronoUnit.MINUTES);

        List<ClickEvent> events = clickEventRepository.findEventsInRange(bucketStart, now);
        if (events.isEmpty()) return;

        // Group by link_id
        Map<Long, List<ClickEvent>> grouped = events.stream()
                .filter(e -> e.getLinkId() != null)
                .collect(Collectors.groupingBy(ClickEvent::getLinkId));

        for (Map.Entry<Long, List<ClickEvent>> entry : grouped.entrySet()) {
            Long linkId = entry.getKey();
            List<ClickEvent> linkEvents = entry.getValue();

            // Đếm unique visitors bằng visitor_hash (set)
            long uniqueVisitors = linkEvents.stream()
                    .map(ClickEvent::getVisitorHash)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    .size();

            long botClicks = linkEvents.stream().filter(ClickEvent::isBot).count();

            // Đếm theo country, device, referrer
            String countryCounts = countToJson(linkEvents, ClickEvent::getCountryCode);
            String deviceCounts = countToJson(linkEvents, ClickEvent::getDeviceType);
            String referrerCounts = countToJson(linkEvents, ClickEvent::getRefererDomain);

            ClickAggMinute agg = ClickAggMinute.builder()
                    .linkId(linkId)
                    .bucketMinute(bucketStart)
                    .totalClicks(linkEvents.size())
                    .uniqueVisitors(uniqueVisitors)
                    .botClicks(botClicks)
                    .countryCounts(countryCounts)
                    .deviceCounts(deviceCounts)
                    .referrerCounts(referrerCounts)
                    .updatedAt(Instant.now())
                    .build();

            clickAggMinuteRepository.save(agg);
        }

        log.debug("Minute aggregation completed: {} links, {} events", grouped.size(), events.size());
    }

    /**
     * Job chạy lúc 00:05 hàng ngày — gộp click_agg_minute của ngày hôm trước thành click_agg_daily.
     *
     * Logic:
     * 1. Xác định ngày hôm trước.
     * 2. Query tất cả click_agg_minute trong ngày đó.
     * 3. Group by link_id → SUM tất cả metrics.
     * 4. UPSERT vào click_agg_daily.
     */
    @Scheduled(cron = "0 5 0 * * *")  // 00:05 hàng ngày
    @Transactional
    public void dailyAggregation() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        Instant dayStart = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = yesterday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<ClickAggMinute> minuteAggs = clickAggMinuteRepository
                .findByBucketMinuteBetween(dayStart, dayEnd);

        if (minuteAggs.isEmpty()) return;

        // Group by link_id → SUM
        Map<Long, List<ClickAggMinute>> grouped = minuteAggs.stream()
                .collect(Collectors.groupingBy(ClickAggMinute::getLinkId));

        for (Map.Entry<Long, List<ClickAggMinute>> entry : grouped.entrySet()) {
            Long linkId = entry.getKey();
            List<ClickAggMinute> aggs = entry.getValue();

            long totalClicks = aggs.stream().mapToLong(ClickAggMinute::getTotalClicks).sum();
            long uniqueVisitors = aggs.stream().mapToLong(ClickAggMinute::getUniqueVisitors).sum();
            long botClicks = aggs.stream().mapToLong(ClickAggMinute::getBotClicks).sum();

            ClickAggDaily daily = ClickAggDaily.builder()
                    .linkId(linkId)
                    .day(yesterday)
                    .totalClicks(totalClicks)
                    .uniqueVisitors(uniqueVisitors)
                    .botClicks(botClicks)
                    .countryCounts("{}")  // Simplified — có thể merge JSONB nếu cần
                    .deviceCounts("{}")
                    .referrerCounts("{}")
                    .updatedAt(Instant.now())
                    .build();

            clickAggDailyRepository.save(daily);
        }

        log.info("Daily aggregation completed for {}: {} links", yesterday, grouped.size());
    }

    // ── Helper: count occurrences → JSON string ──

    /**
     * Đếm số lần xuất hiện của mỗi giá trị → JSON string.
     * VD: ["VN", "VN", "US"] → {"VN": 2, "US": 1}
     */
    private <T> String countToJson(List<ClickEvent> events,
                                   java.util.function.Function<ClickEvent, String> extractor) {
        Map<String, Long> counts = events.stream()
                .map(extractor)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        if (counts.isEmpty()) return "{}";

        // Build JSON thủ công (tránh dependency Jackson ở đây)
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey().replace("\"", "\\\"")).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
