package com.ThanhND05.url_shortener.analytics.repository;

import com.ThanhND05.url_shortener.analytics.entity.LinkCounter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LinkCounterRepository extends JpaRepository<LinkCounter, Long> {

    /**
     * Atomic increment click counter cho 1 link.
     * Dùng INSERT ... ON CONFLICT (UPSERT) để tránh race condition.
     * Nếu chưa có row → INSERT với total_clicks=1.
     * Nếu đã có → INCREMENT total_clicks + cập nhật last_clicked_at.
     */
    @Modifying
    @Query(value = """
        INSERT INTO analytics.link_counters (link_id, total_clicks, unique_visitors_estimate, last_clicked_at, updated_at)
        VALUES (:linkId, 1, 0, :clickedAt, now())
        ON CONFLICT (link_id) DO UPDATE SET
            total_clicks = analytics.link_counters.total_clicks + 1,
            last_clicked_at = :clickedAt,
            updated_at = now()
    """, nativeQuery = true)
    void incrementClickCount(Long linkId, Instant clickedAt);

    // ── ADMIN QUERIES ────────────────────────────────────

    /** Tổng clicks toàn hệ thống. */
    @Query("SELECT COALESCE(SUM(lc.totalClicks), 0) FROM LinkCounter lc")
    long sumTotalClicks();

    /** Top N links theo total clicks (JOIN với Link entity để lấy thông tin). */
    List<LinkCounter> findTopByOrderByTotalClicksDesc(Pageable pageable);
}

