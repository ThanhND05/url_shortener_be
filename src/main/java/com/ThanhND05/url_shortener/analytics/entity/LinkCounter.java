package com.ThanhND05.url_shortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity đại diện cho bảng analytics.link_counters — counter tổng hợp cho mỗi link.
 *
 * Cơ chế hoạt động:
 * - Mỗi khi có click event, ClickIngestionService UPSERT (increment) bảng này.
 * - Cung cấp tổng click + unique visitors + last_clicked_at cho dashboard real-time.
 * - Đồng thời sync ngược lại link.links.click_count và link.redirect_lookup.click_count.
 *
 * Tại sao tách riêng bảng counters?
 * - Tránh lock contention trên bảng links (nhiều concurrent writes).
 * - Counter riêng → lightweight upsert, không ảnh hưởng link metadata.
 * - Dashboard chỉ cần query bảng nhỏ này thay vì scan click_events.
 */
@Entity
@Table(name = "link_counters", schema = "analytics")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkCounter {

    /** PK = link_id (1:1 với link.links.id). */
    @Id
    @Column(name = "link_id")
    private Long linkId;

    @Column(name = "total_clicks", nullable = false)
    @Builder.Default
    private long totalClicks = 0;

    /** Ước lượng unique visitors — không chính xác 100% do hash collision. */
    @Column(name = "unique_visitors_estimate", nullable = false)
    @Builder.Default
    private long uniqueVisitorsEstimate = 0;

    @Column(name = "last_clicked_at")
    private Instant lastClickedAt;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
