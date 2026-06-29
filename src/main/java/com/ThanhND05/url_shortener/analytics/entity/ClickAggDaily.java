package com.ThanhND05.url_shortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity đại diện cho bảng analytics.click_agg_daily — dữ liệu click gộp theo ngày.
 *
 * Cơ chế hoạt động:
 * - Scheduled job (chạy 1 lần/ngày vào 00:05) đọc click_agg_minute của ngày hôm trước
 *   → SUM lại → UPSERT vào bảng này.
 * - Dùng cho biểu đồ dài hạn (30 ngày, 90 ngày, ...).
 * - Không partition (data nhỏ: 1 row/link/ngày).
 */
@Entity
@Table(name = "click_agg_daily", schema = "analytics")
@IdClass(ClickAggDailyId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAggDaily {

    @Id
    @Column(name = "link_id")
    private Long linkId;

    @Id
    @Column(name = "day")
    private LocalDate day;

    @Column(name = "total_clicks", nullable = false)
    @Builder.Default
    private long totalClicks = 0;

    @Column(name = "unique_visitors", nullable = false)
    @Builder.Default
    private long uniqueVisitors = 0;

    @Column(name = "bot_clicks", nullable = false)
    @Builder.Default
    private long botClicks = 0;

    /** JSONB: {"VN": 500, "US": 200} */
    @Column(name = "country_counts", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String countryCounts = "{}";

    /** JSONB: {"mobile": 400, "desktop": 250} */
    @Column(name = "device_counts", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String deviceCounts = "{}";

    /** JSONB: {"facebook.com": 150, "direct": 100} */
    @Column(name = "referrer_counts", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String referrerCounts = "{}";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
