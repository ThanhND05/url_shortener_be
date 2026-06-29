package com.ThanhND05.url_shortener.iam.entity;

import com.ThanhND05.url_shortener.iam.enums.SystemRole;
import com.ThanhND05.url_shortener.iam.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng iam.users — lưu thông tin tài khoản người dùng.
 *
 * Cơ chế hoạt động:
 * - Mỗi user có email duy nhất (CITEXT = case-insensitive trong PostgreSQL).
 * - password_hash: mật khẩu đã được BCrypt hash, KHÔNG BAO GIỜ lưu plain text.
 * - system_role: chỉ dùng lúc tạo user để bootstrap RBAC (gán role tương ứng).
 *   Lúc runtime, hệ thống check quyền qua bảng user_roles + role_permissions.
 * - Soft delete: khi xóa user, set status = DELETED và deleted_at = now(),
 *   dữ liệu vẫn giữ lại để audit/recovery.
 * - created_at/updated_at: tự động fill bởi JPA Auditing.
 */
@Entity
@Table(name = "users", schema = "iam")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Email (case-insensitive nhờ CITEXT ở DB) — dùng làm username đăng nhập. */
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    /** Mật khẩu đã hash bằng BCrypt — không bao giờ trả ra ngoài API. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Tên hiển thị trên UI (không bắt buộc). */
    @Column(name = "display_name", length = 150)
    private String displayName;

    /** URL ảnh đại diện (có thể null nếu chưa upload). */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Vai trò hệ thống — chỉ dùng khi TẠO user để tự động gán role RBAC.
     * VD: SUPER_ADMIN → gán role "super_admin"; USER → gán role "member".
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, length = 30)
    @Builder.Default
    private SystemRole systemRole = SystemRole.USER;

    /** Trạng thái tài khoản: ACTIVE / LOCKED / DELETED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /** Thời điểm soft-delete (null nếu chưa xóa). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
