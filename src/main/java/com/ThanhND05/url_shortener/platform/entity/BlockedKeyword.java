package com.ThanhND05.url_shortener.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity đại diện cho bảng platform.blocked_keywords — từ khóa bị cấm trong slug/URL.
 *
 * Cơ chế hoạt động:
 * - Admin thêm keyword (VD: "admin", "login", "api", "phishing").
 * - Khi user tạo short link với custom slug, LinkService check:
 *   + slug chứa blocked keyword → reject.
 *   + original_url chứa blocked keyword → reject.
 * - Case-insensitive nhờ CITEXT.
 *
 * Khác với BlockedDomain (chặn theo domain), BlockedKeyword chặn theo nội dung text.
 */
@Entity
@Table(name = "blocked_keywords", schema = "platform")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Từ khóa bị cấm (CITEXT = case-insensitive). */
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String keyword;

    /** Lý do cấm. */
    @Column(name = "reason")
    private String reason;

    /** Người tạo (email admin). */
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
