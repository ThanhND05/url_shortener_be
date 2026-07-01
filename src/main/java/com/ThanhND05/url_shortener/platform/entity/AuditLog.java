package com.ThanhND05.url_shortener.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity đại diện cho bảng platform.audit_logs — nhật ký hành động trong hệ
 * thống.
 *
 * Cơ chế hoạt động:
 * - AuditEventListener lắng nghe TẤT CẢ domain events (UserCreated,
 * LinkCreated, PasswordChanged, ...).
 * - Mỗi event → INSERT 1 audit log record.
 * - Admin có thể query audit log để xem lịch sử: ai làm gì, lúc nào, trên
 * resource nào.
 *
 * Các trường quan trọng:
 * - actor_id: user thực hiện hành động (null nếu system action).
 * - action: "USER_CREATED", "LINK_CREATED", "PASSWORD_CHANGED",
 * "ACCOUNT_LOCKED".
 * - resource_type + resource_id: đối tượng bị tác động (VD: "Link",
 * "uuid-123").
 * - metadata (JSONB): thông tin bổ sung tùy event (VD: old email, new status).
 */
@Entity
@Table(name = "audit_logs", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID user thực hiện hành động (null = system). */
    @Column(name = "actor_id")
    private UUID actorId;

    /** Hành động: "USER_CREATED", "LINK_CREATED", "PASSWORD_CHANGED", ... */
    @Column(name = "action", nullable = false, length = 120)
    private String action;

    /** Loại resource bị tác động: "User", "Link", "Domain", "Role". */
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    /** ID resource bị tác động (string để linh hoạt: UUID hoặc BIGINT). */
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "ip_hash")
    private byte[] ipHash;

    @Column(name = "user_agent_hash")
    private byte[] userAgentHash;

    /** Custom metadata mở rộng (JSONB). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
