package com.ThanhND05.url_shortener.iam.entity;

import com.ThanhND05.url_shortener.iam.enums.ScopeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng iam.user_roles — gán vai trò cho người dùng.
 *
 * Cơ chế hoạt động:
 * - Mỗi bản ghi gán 1 role cho 1 user với phạm vi (scope) cụ thể.
 * - scope_type = GLOBAL:    quyền áp dụng toàn hệ thống (scope_id = null).
 * - scope_type = WORKSPACE: quyền chỉ có hiệu lực trong workspace cụ thể.
 * - expires_at: nếu != null, role sẽ tự hết hiệu lực sau thời gian này
 *   (hữu ích cho trial access, temporary promotion).
 * - granted_by: UUID của admin đã gán role này (audit trail).
 *
 * Unique constraint: (user_id, role_id, scope_type, scope_id) — một user
 * chỉ có đúng một bản ghi cho mỗi role+scope, tránh gán trùng.
 */
@Entity
@Table(name = "user_roles", schema = "iam",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "role_id", "scope_type", "scope_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Quan hệ tới Role — EAGER vì khi load UserRole luôn cần biết role (và permissions). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** Phạm vi áp dụng: GLOBAL hoặc WORKSPACE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    @Builder.Default
    private ScopeType scopeType = ScopeType.GLOBAL;

    /** UUID của workspace nếu scope = WORKSPACE, null nếu GLOBAL. */
    @Column(name = "scope_id")
    private UUID scopeId;

    /** UUID admin đã gán role này. */
    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant grantedAt = Instant.now();

    /** Thời điểm hết hiệu lực — null = vĩnh viễn. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Kiểm tra role assignment còn hiệu lực (chưa hết hạn). */
    public boolean isActive() {
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}
