package com.ThanhND05.url_shortener.analytics.service;

import com.ThanhND05.url_shortener.analytics.dto.response.*;
import com.ThanhND05.url_shortener.analytics.entity.*;
import com.ThanhND05.url_shortener.analytics.repository.*;
import com.ThanhND05.url_shortener.common.exception.ResourceNotFoundException;
import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service query analytics data — phục vụ API cho dashboard.
 *
 * Cung cấp 3 loại query:
 * 1. getStats()     — tổng hợp counters + timeseries cho 1 link.
 * 2. getTimeseries()— timeseries chi tiết (minute hoặc daily, tùy time range).
 * 3. getClickLog()  — danh sách raw click events (phân trang).
 *
 * Logic chọn granularity tự động:
 * - Range ≤ 24h → dùng click_agg_minute (1440 data points max).
 * - Range > 24h → dùng click_agg_daily (90 data points max cho 90 ngày).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final LinkRepository linkRepository;
    private final LinkCounterRepository linkCounterRepository;
    private final ClickAggMinuteRepository clickAggMinuteRepository;
    private final ClickAggDailyRepository clickAggDailyRepository;
    private final ClickEventRepository clickEventRepository;

    /**
     * Lấy stats tổng hợp cho 1 link: counters + timeseries mặc định 24h.
     *
     * @param linkPublicId public UUID của link.
     * @return LinkStatsResponse với counters + timeseries.
     */
    public LinkStatsResponse getStats(UUID linkPublicId) {
        Link link = linkRepository.findByPublicId(linkPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Link", "publicId", linkPublicId));

        // Lấy counters (có thể chưa có nếu link chưa được click)
        LinkCounter counter = linkCounterRepository.findById(link.getId())
                .orElse(LinkCounter.builder()
                        .linkId(link.getId()).totalClicks(0).uniqueVisitorsEstimate(0).build());

        // Timeseries mặc định: 24h qua, granularity phút
        Instant from = Instant.now().minus(24, ChronoUnit.HOURS);
        List<ClickAggResponse> timeseries = getMinuteTimeseries(link.getId(), from, Instant.now());

        return LinkStatsResponse.from(counter, timeseries);
    }

    /**
     * Lấy timeseries cho 1 link trong khoảng thời gian.
     * Tự động chọn granularity: ≤24h → phút, >24h → ngày.
     */
    public List<ClickAggResponse> getTimeseries(UUID linkPublicId, Instant from, Instant to) {
        Link link = linkRepository.findByPublicId(linkPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Link", "publicId", linkPublicId));

        long hours = ChronoUnit.HOURS.between(from, to);
        if (hours <= 24) {
            return getMinuteTimeseries(link.getId(), from, to);
        } else {
            return getDailyTimeseries(link.getId(),
                    from.atZone(ZoneOffset.UTC).toLocalDate(),
                    to.atZone(ZoneOffset.UTC).toLocalDate());
        }
    }

    /**
     * Lấy raw click log cho 1 link (phân trang).
     * Dùng cho bảng "Recent Clicks" trong dashboard.
     */
    public Page<ClickEventResponse> getClickLog(UUID linkPublicId, Instant from, Instant to, Pageable pageable) {
        Link link = linkRepository.findByPublicId(linkPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Link", "publicId", linkPublicId));

        return clickEventRepository.findByLinkIdAndOccurredAtBetween(
                        link.getId(), from, to, pageable)
                .map(this::toClickEventResponse);
    }

    // ── Private helpers ──────────────────────────────────

    private List<ClickAggResponse> getMinuteTimeseries(Long linkId, Instant from, Instant to) {
        return clickAggMinuteRepository
                .findByLinkIdAndBucketMinuteBetweenOrderByBucketMinuteAsc(linkId, from, to)
                .stream().map(agg -> ClickAggResponse.builder()
                        .bucket(agg.getBucketMinute())
                        .totalClicks(agg.getTotalClicks())
                        .uniqueVisitors(agg.getUniqueVisitors())
                        .botClicks(agg.getBotClicks())
                        .countryCounts(agg.getCountryCounts())
                        .deviceCounts(agg.getDeviceCounts())
                        .referrerCounts(agg.getReferrerCounts())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ClickAggResponse> getDailyTimeseries(Long linkId, LocalDate from, LocalDate to) {
        return clickAggDailyRepository
                .findByLinkIdAndDayBetweenOrderByDayAsc(linkId, from, to)
                .stream().map(agg -> ClickAggResponse.builder()
                        .bucket(agg.getDay().atStartOfDay(ZoneOffset.UTC).toInstant())
                        .totalClicks(agg.getTotalClicks())
                        .uniqueVisitors(agg.getUniqueVisitors())
                        .botClicks(agg.getBotClicks())
                        .countryCounts(agg.getCountryCounts())
                        .deviceCounts(agg.getDeviceCounts())
                        .referrerCounts(agg.getReferrerCounts())
                        .build())
                .collect(Collectors.toList());
    }

    private ClickEventResponse toClickEventResponse(ClickEvent event) {
        return ClickEventResponse.builder()
                .eventId(event.getEventId())
                .occurredAt(event.getOccurredAt())
                .countryCode(event.getCountryCode())
                .region(event.getRegion())
                .city(event.getCity())
                .deviceType(event.getDeviceType())
                .os(event.getOs())
                .browser(event.getBrowser())
                .refererDomain(event.getRefererDomain())
                .isBot(event.isBot())
                .build();
    }
}
