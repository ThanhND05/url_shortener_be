package com.ThanhND05.url_shortener.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity đại diện cho bảng iam.roles — định nghĩa các vai trò trong hệ thống.
 *
 * Cơ chế RBAC (Role-Based Access Control):
 * - Mỗi Role có nhiều Permission (quan hệ M:N qua bảng role_permissions).
 * - User được gán Role thông qua bảng user_roles (có thể scoped).
 * - Hệ thống có 4 system roles mặc định: super_admin, admin, member, viewer.
 * - System roles (is_system = true) không được phép xóa hoặc đổi tên.
 * - Admin có thể tạo custom roles (is_system = false) và gán permissions tùy ý.
 *
 * Ví dụ: Role "admin" có permissions: link:create, link:read, link:update, link:delete,
 *         user:read, user:manage, analytics:read, ...
 */
@Entity
@Table(name = "roles", schema = "iam")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Slug duy nhất: 'super_admin', 'admin', 'editor', 'viewer', ... */
    @Column(nullable = false, unique = true, length = 80)
    private String name;

    /** Tên hiển thị trên UI: 'Super Admin', 'Admin', ... */
    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "description")
    private String description;

    /** true = role hệ thống (không được xóa/sửa tên), false = role tùy chỉnh. */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    /**
     * Các permissions thuộc role này — quan hệ M:N qua bảng iam.role_permissions.
     * Dùng Set để tránh trùng lặp. FetchType.EAGER vì danh sách permissions
     * thường nhỏ và luôn cần khi load role.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions", schema = "iam",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
