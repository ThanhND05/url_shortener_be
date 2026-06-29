package com.ThanhND05.url_shortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity đại diện cho bảng analytics.click_agg_minute — dữ liệu click gộp theo phút.
 *
 * Cơ chế hoạt động:
 * - ClickIngestionService ghi raw event vào click_events.
 * - Scheduled job (mỗi phút) đọc click_events → GROUP BY link_id, trunc(occurred_at, minute)
 *   → UPSERT vào bảng này.
 * - Lưu tổng click, unique visitors, bot clicks + breakdowns (country, device, referrer).
 * - Bảng này cũng dùng PARTITION BY RANGE — giữ 7 ngày gần nhất, data cũ hơn roll-up vào daily.
 *
 * Tại sao gộp theo phút?
 * - Query real-time dashboard nhanh hơn đọc raw events.
 * - Giảm I/O khi vẽ biểu đồ timeseries (1440 row/ngày/link thay vì hàng triệu events).
 */
@Entity
@Table(name = "click_agg_minute", schema = "analytics")
@IdClass(ClickAggMinuteId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAggMinute {

    @Id
    @Column(name = "link_id")
    private Long linkId;

    /** Thời điểm đầu phút (VD: 2026-06-10T09:15:00Z). */
    @Id
    @Column(name = "bucket_minute")
    private Instant bucketMinute;

    @Column(name = "total_clicks", nullable = false)
    @Builder.Default
    private long totalClicks = 0;

    @Column(name = "unique_visitors", nullable = false)
    @Builder.Default
    private long uniqueVisitors = 0;

    @Column(name = "bot_clicks", nullable = false)
    @Builder.Default
    private long botClicks = 0;

    /** JSONB: {"VN": 120, "US": 45, "JP": 30} */
    @Column(name = "country_counts", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String countryCounts = "{}";

    /** JSONB: {"mobile": 100, "desktop": 80, "tablet": 15} */
    @Column(name = "device_counts", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String deviceCounts = "{}";

    /** JSONB: {"facebook.com": 50, "google.com": 30, "direct": 25} */
    @Column(name = "referrer_counts", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String referrerCounts = "{}";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
