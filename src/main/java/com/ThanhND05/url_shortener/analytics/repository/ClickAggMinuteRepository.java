package com.ThanhND05.url_shortener.analytics.repository;

import com.ThanhND05.url_shortener.analytics.entity.ClickAggMinute;
import com.ThanhND05.url_shortener.analytics.entity.ClickAggMinuteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickAggMinuteRepository extends JpaRepository<ClickAggMinute, ClickAggMinuteId> {

    /** Lấy timeseries theo phút cho 1 link (dùng cho real-time chart). */
    List<ClickAggMinute> findByLinkIdAndBucketMinuteBetweenOrderByBucketMinuteAsc(
            Long linkId, Instant from, Instant to);

    /** Lấy tất cả aggregate của 1 ngày (dùng cho daily roll-up job). */
    List<ClickAggMinute> findByBucketMinuteBetween(Instant from, Instant to);
}
