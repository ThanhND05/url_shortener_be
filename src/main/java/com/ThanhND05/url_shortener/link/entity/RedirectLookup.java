package com.ThanhND05.url_shortener.link.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng link.redirect_lookup — bảng denormalized cho hot path redirect.
 *
 * Cơ chế hoạt động:
 * - Đây là bảng "read-optimized" — chỉ chứa đúng các trường cần cho redirect.
 * - Redirect Service CHỈ ĐỌC bảng này (không đọc link.links) → giảm join, tăng tốc.
 * - Application tự sync data từ link.links vào đây mỗi khi INSERT/UPDATE link.
 *   (KHÔNG dùng DB trigger — app layer kiểm soát logic sync).
 * - Kết hợp Redis cache: key "redirect:{domainId}:{shortCode}" → cache object này.
 *
 * Flow redirect:
 *   1. Request GET /r/{shortCode} → RedirectService
 *   2. Check Redis cache → nếu hit → dùng luôn
 *   3. Cache miss → query redirect_lookup → cache kết quả
 *   4. Check: status == ACTIVE, chưa hết hạn, chưa quá max_clicks, password?
 *   5. OK → HTTP 302 redirect → publish LinkClickedEvent (async)
 */
@Entity
@Table(name = "redirect_lookup", schema = "link")
@IdClass(RedirectLookupId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedirectLookup {

    @Id
    @Column(name = "domain_id")
    private Long domainId;

    @Id
    @Column(name = "short_code", length = 64)
    private String shortCode;

    /** Internal link ID — dùng để ghi analytics event. */
    @Column(name = "link_id", nullable = false)
    private Long linkId;

    /** Public UUID của link — dùng trong analytics event. */
    @Column(name = "link_public_id", nullable = false)
    private UUID linkPublicId;

    /** URL gốc — đích redirect. */
    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    /** Status copy từ link.links — check trước khi redirect. */
    @Column(nullable = false, length = 30)
    private String status;

    /** HTTP status code cho redirect (301/302/307/308). */
    @Column(name = "redirect_type", nullable = false)
    @Builder.Default
    private short redirectType = 302;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "max_clicks")
    private Long maxClicks;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private long clickCount = 0;

    /** true nếu link yêu cầu mật khẩu — redirect service cần yêu cầu nhập password. */
    @Column(name = "password_required", nullable = false)
    @Builder.Default
    private boolean passwordRequired = false;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
