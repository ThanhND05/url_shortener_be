package com.ThanhND05.url_shortener.link.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng link.tags — nhãn phân loại cho short links.
 *
 * - Mỗi user tạo tag riêng (owner_id).
 * - Tên tag unique trong phạm vi user (unique index trên owner_id + lower(name)).
 * - Quan hệ M:N với Link qua bảng link_tags (đã map ở Link entity bằng @ManyToMany).
 * - Dùng để lọc, nhóm, và quản lý links theo chủ đề/chiến dịch.
 *
 * Ví dụ: Tag "summer-sale-2026", "social-media", "email-campaign".
 */
@Entity
@Table(name = "tags", schema = "link")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
