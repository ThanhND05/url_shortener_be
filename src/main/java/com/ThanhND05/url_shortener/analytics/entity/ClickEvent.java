package com.ThanhND05.url_shortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity đại diện cho bảng analytics.click_events — ghi nhận mỗi lượt click.
 *
 * Cơ chế hoạt động:
 * - Khi RedirectService redirect thành công → publish LinkClickedEvent.
 * - ClickIngestionService (listener) nhận event → INSERT vào bảng này.
 * - Bảng này dùng PARTITION BY RANGE (occurred_at) → mỗi tháng 1 partition.
 *   (VD: analytics.click_events_2026_06 chứa data tháng 6/2026).
 * - Composite PK (occurred_at, event_id) — cần cho partitioned table.
 *
 * Các trường phân tích:
 * - ip_hash + visitor_hash: tracking unique visitor (privacy-safe, không lưu raw IP).
 * - country_code, region, city: geo-location từ IP (GeoIP lookup).
 * - device_type, os, browser: parse từ User-Agent header.
 * - referer_domain: nguồn traffic (VD: "facebook.com", "google.com").
 * - is_bot: phát hiện bot/crawler qua User-Agent pattern.
 * - latency_ms: thời gian xử lý redirect (performance monitoring).
 */
@Entity
@Table(name = "click_events", schema = "analytics")
@IdClass(ClickEventId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    /** Thời điểm xảy ra click — phần PK thứ nhất (partition key). */
    @Id
    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    /** UUID event — phần PK thứ hai. */
    @Id
    @Column(name = "event_id", nullable = false)
    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    /** FK tới link.links.id — dùng cho aggregation. */
    @Column(name = "link_id")
    private Long linkId;

    /** Public UUID của link — dùng cho API response. */
    @Column(name = "link_public_id")
    private UUID linkPublicId;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "short_code", length = 64)
    private String shortCode;

    /** SHA-256 hash IP client — privacy-safe tracking. */
    @Column(name = "ip_hash")
    private byte[] ipHash;

    /**
     * Hash kết hợp IP + User-Agent — xấp xỉ unique visitor.
     * Cùng IP + UA = cùng visitor (không 100% chính xác nhưng tốt cho analytics).
     */
    @Column(name = "visitor_hash")
    private byte[] visitorHash;

    @Column(name = "user_agent_hash")
    private byte[] userAgentHash;

    /** Raw referer URL — nguồn traffic. */
    @Column(name = "referer")
    private String referer;

    /** Domain trích xuất từ referer (VD: "facebook.com"). */
    @Column(name = "referer_domain", length = 255)
    private String refererDomain;

    // ── Geo-location (parse từ IP, để null nếu chưa có GeoIP) ──

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "city", length = 100)
    private String city;

    // ── Device info (parse từ User-Agent) ──

    /** Loại thiết bị: "desktop", "mobile", "tablet". */
    @Column(name = "device_type", length = 30)
    private String deviceType;

    @Column(name = "os", length = 80)
    private String os;

    @Column(name = "browser", length = 80)
    private String browser;

    /** true nếu User-Agent pattern match bot/crawler. */
    @Column(name = "is_bot", nullable = false)
    @Builder.Default
    private boolean isBot = false;

    /** Request ID cho distributed tracing. */
    @Column(name = "request_id")
    private UUID requestId;

    /** HTTP status code của redirect response (301/302/...). */
    @Column(name = "http_status")
    private Short httpStatus;

    /** Thời gian xử lý redirect (ms) — performance tracking. */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /** Custom metadata mở rộng (JSONB). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadata = "{}";
}
