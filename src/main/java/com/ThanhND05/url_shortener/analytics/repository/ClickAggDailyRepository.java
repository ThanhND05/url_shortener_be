package com.ThanhND05.url_shortener.analytics.repository;

import com.ThanhND05.url_shortener.analytics.entity.ClickAggDaily;
import com.ThanhND05.url_shortener.analytics.entity.ClickAggDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClickAggDailyRepository extends JpaRepository<ClickAggDaily, ClickAggDailyId> {

    /** Lấy timeseries theo ngày cho 1 link (dùng cho 30-day / 90-day chart). */
    List<ClickAggDaily> findByLinkIdAndDayBetweenOrderByDayAsc(
            Long linkId, LocalDate from, LocalDate to);

    // ── ADMIN SYSTEM-WIDE QUERIES ────────────────────────

    /**
     * Timeseries tổng click toàn hệ thống gộp theo ngày.
     * SUM(total_clicks), SUM(unique_visitors), SUM(bot_clicks) GROUP BY day.
     * Dùng cho admin dashboard chart.
     *
     * @return List<Object[]> mỗi phần tử: [day (LocalDate), totalClicks (Long), uniqueVisitors (Long), botClicks (Long)]
     */
    @Query("""
        SELECT d.day, SUM(d.totalClicks), SUM(d.uniqueVisitors), SUM(d.botClicks)
        FROM ClickAggDaily d
        WHERE d.day >= :from AND d.day <= :to
        GROUP BY d.day
        ORDER BY d.day ASC
    """)
    List<Object[]> systemWideDailyTimeseries(LocalDate from, LocalDate to);
}
