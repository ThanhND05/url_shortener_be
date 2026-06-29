package com.ThanhND05.url_shortener.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity đại diện cho bảng platform.blocked_domains — danh sách domain bị chặn.
 *
 * Cơ chế hoạt động:
 * - Admin thêm domain vào blacklist (VD: phishing, malware, spam).
 * - Khi user tạo short link, LinkService check: original_url chứa blocked domain → reject.
 * - Có thể import từ nguồn bên ngoài (Google Safe Browsing, PhishTank, ...).
 *
 * Ví dụ: "evil-phishing.com" → bất kỳ link nào trỏ tới domain này đều bị từ chối.
 */
@Entity
@Table(name = "blocked_domains", schema = "platform")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Domain bị chặn (CITEXT = case-insensitive). */
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String domain;

    /** Lý do chặn. */
    @Column(name = "reason")
    private String reason;

    /** Nguồn: "manual", "google_safe_browsing", "phishtank". */
    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
