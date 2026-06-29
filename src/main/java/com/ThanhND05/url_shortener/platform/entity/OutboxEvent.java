package com.ThanhND05.url_shortener.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng platform.outbox_events — Transactional Outbox Pattern.
 *
 * Cơ chế hoạt động:
 * - Khi cần publish event ra hệ thống ngoài (webhook, message queue), KHÔNG gọi trực tiếp.
 * - Thay vào đó, INSERT event vào bảng outbox TRONG CÙNG transaction với business logic.
 * - Một poller (scheduled job) đọc các event chưa publish → gửi đi → cập nhật published_at.
 *
 * Tại sao dùng Outbox Pattern?
 * - Đảm bảo at-least-once delivery — nếu crash trước khi gửi, event vẫn trong DB.
 * - Tránh distributed transaction giữa DB và message broker.
 * - retry_count + last_error cho dead-letter handling.
 *
 * Ví dụ: Khi tạo link, outbox event có thể trigger webhook thông báo cho user.
 */
@Entity
@Table(name = "outbox_events", schema = "platform")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column(name = "id")
    @Builder.Default
    private UUID id = UUID.randomUUID();

    /** Loại aggregate gốc: "User", "Link", "Domain". */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /** ID của aggregate gốc (VD: user UUID, link publicId). */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /** Tên event: "UserCreated", "LinkCreated", "PasswordChanged". */
    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    /** Payload JSON chứa data event. */
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** null = chưa publish, có giá trị = đã gửi thành công. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_error")
    private String lastError;
}
