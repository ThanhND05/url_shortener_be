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
}
