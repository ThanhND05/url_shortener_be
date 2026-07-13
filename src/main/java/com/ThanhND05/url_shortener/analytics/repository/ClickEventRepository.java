package com.ThanhND05.url_shortener.analytics.repository;

import com.ThanhND05.url_shortener.analytics.entity.ClickEvent;
import com.ThanhND05.url_shortener.analytics.entity.ClickEventId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, ClickEventId> {

    /** Lấy click events của 1 link trong khoảng thời gian (phân trang). */
    Page<ClickEvent> findByLinkIdAndOccurredAtBetween(
            Long linkId, Instant from, Instant to, Pageable pageable);

    /** Đếm tổng click của 1 link trong khoảng thời gian. */
    long countByLinkIdAndOccurredAtBetween(Long linkId, Instant from, Instant to);

    /** Lấy raw events chưa được aggregate (dùng cho scheduled aggregation job). */
    @Query("""
        SELECT ce FROM ClickEvent ce
        WHERE ce.occurredAt >= :from AND ce.occurredAt < :to
        ORDER BY ce.linkId, ce.occurredAt
    """)
    List<ClickEvent> findEventsInRange(Instant from, Instant to);

    // ── ADMIN AGGREGATE QUERIES ──────────────────────────

    /** Tổng click events toàn hệ thống trong khoảng thời gian. */
    long countByOccurredAtBetween(Instant from, Instant to);

    /** Breakdown theo device type (mobile, desktop, tablet). */
    @Query("""
        SELECT ce.deviceType, COUNT(ce) FROM ClickEvent ce
        WHERE ce.occurredAt >= :from AND ce.occurredAt < :to
          AND ce.deviceType IS NOT NULL
        GROUP BY ce.deviceType
        ORDER BY COUNT(ce) DESC
    """)
    List<Object[]> countByDeviceType(Instant from, Instant to);

    /** Breakdown theo OS. */
    @Query("""
        SELECT ce.os, COUNT(ce) FROM ClickEvent ce
        WHERE ce.occurredAt >= :from AND ce.occurredAt < :to
          AND ce.os IS NOT NULL
        GROUP BY ce.os
        ORDER BY COUNT(ce) DESC
    """)
    List<Object[]> countByOs(Instant from, Instant to);

    /** Breakdown theo browser. */
    @Query("""
        SELECT ce.browser, COUNT(ce) FROM ClickEvent ce
        WHERE ce.occurredAt >= :from AND ce.occurredAt < :to
          AND ce.browser IS NOT NULL
        GROUP BY ce.browser
        ORDER BY COUNT(ce) DESC
    """)
    List<Object[]> countByBrowser(Instant from, Instant to);

    /** Breakdown theo quốc gia. */
    @Query("""
        SELECT ce.countryCode, COUNT(ce) FROM ClickEvent ce
        WHERE ce.occurredAt >= :from AND ce.occurredAt < :to
          AND ce.countryCode IS NOT NULL
        GROUP BY ce.countryCode
        ORDER BY COUNT(ce) DESC
    """)
    List<Object[]> countByCountry(Instant from, Instant to);

    /** Breakdown theo nguồn referrer. */
    @Query("""
        SELECT ce.refererDomain, COUNT(ce) FROM ClickEvent ce
        WHERE ce.occurredAt >= :from AND ce.occurredAt < :to
          AND ce.refererDomain IS NOT NULL
        GROUP BY ce.refererDomain
        ORDER BY COUNT(ce) DESC
    """)
    List<Object[]> countByReferrer(Instant from, Instant to);
}

