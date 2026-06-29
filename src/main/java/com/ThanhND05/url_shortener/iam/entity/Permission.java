package com.ThanhND05.url_shortener.iam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity đại diện cho bảng iam.permissions — định nghĩa quyền hành động trên tài nguyên.
 *
 * Mỗi permission là một cặp (resource, action):
 * - resource: loại tài nguyên ('link', 'domain', 'analytics', 'user', 'billing').
 * - action:   hành động ('create', 'read', 'update', 'delete', 'export', 'manage').
 *
 * Permission slug = resource + ':' + action (VD: "link:create", "analytics:read").
 * Slug này được nhúng vào JWT access token và kiểm tra bằng @PreAuthorize.
 */
@Entity
@Table(name = "permissions", schema = "iam",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource", "action"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên tài nguyên: 'link', 'domain', 'analytics', 'user', 'billing'. */
    @Column(nullable = false, length = 80)
    private String resource;

    /** Hành động: 'create', 'read', 'update', 'delete', 'export', 'manage'. */
    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Trả về slug dạng "resource:action", VD: "link:create". */
    public String toSlug() {
        return resource + ":" + action;
    }
}
