package com.ThanhND05.url_shortener.analytics.repository;

import com.ThanhND05.url_shortener.analytics.entity.ClickAggDaily;
import com.ThanhND05.url_shortener.analytics.entity.ClickAggDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClickAggDailyRepository extends JpaRepository<ClickAggDaily, ClickAggDailyId> {

    /** Lấy timeseries theo ngày cho 1 link (dùng cho 30-day / 90-day chart). */
    List<ClickAggDaily> findByLinkIdAndDayBetweenOrderByDayAsc(
            Long linkId, LocalDate from, LocalDate to);
}
