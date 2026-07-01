package com.ThanhND05.url_shortener.link.entity;

import com.ThanhND05.url_shortener.link.enums.LinkStatus;
import com.ThanhND05.url_shortener.link.enums.ShortCodeType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity đại diện cho bảng link.links — thông tin đầy đủ của một short link.
 *
 * Cơ chế hoạt động:
 * 1. User tạo link → server sinh short_code (Base62 từ sequence hoặc custom).
 * 2. Lưu vào bảng links (đầy đủ metadata) + sync vào redirect_lookup
 * (denormalized, cho hot path).
 * 3. Khi có click → RedirectService đọc từ redirect_lookup (nhanh), không đọc
 * bảng links.
 * 4. click_count và last_clicked_at được cập nhật bất đồng bộ (từ analytics
 * module).
 *
 * Các trường đặc biệt:
 * - public_id (UUID): expose ra API thay vì internal BIGINT id.
 * - original_url_hash (SHA-256): dùng để tìm link trùng URL nhanh.
 * - password_hash: nếu có → link yêu cầu nhập mật khẩu trước khi redirect.
 * - starts_at / expires_at: lên lịch kích hoạt / hết hạn tự động.
 * - max_clicks: giới hạn tổng số lần click (null = không giới hạn).
 * - metadata (JSONB): dữ liệu tùy chỉnh mở rộng (UTM params, custom fields).
 */
@Entity
@Table(name = "links", schema = "link")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    @Builder.Default
    private UUID publicId = UUID.randomUUID();

    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * FK tới domain — xác định prefix URL (VD: domain "go.site.vn" + code "abc" →
     * go.site.vn/abc).
     */
    @Column(name = "domain_id", nullable = false)
    private Long domainId;

    /** Mã ngắn (VD: "q0U", "my-promo"). Unique trong phạm vi domain. */
    @Column(name = "short_code", nullable = false, length = 64)
    private String shortCode;

    /** GENERATED = hệ thống sinh, CUSTOM = user tự chọn. */
    @Enumerated(EnumType.STRING)
    @Column(name = "short_code_type", nullable = false, length = 30)
    @Builder.Default
    private ShortCodeType shortCodeType = ShortCodeType.GENERATED;

    /** URL gốc mà short link trỏ tới. */
    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    /** URL đã chuẩn hóa (lowercase host, loại bỏ trailing slash, ...). */
    @Column(name = "normalized_url")
    private String normalizedUrl;

    /** SHA-256 hex của original_url — dùng index để tìm link trùng nhanh. */
    @Column(name = "original_url_hash", length = 64)
    private String originalUrlHash;

    @Column(length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private LinkStatus status = LinkStatus.ACTIVE;

    /** HTTP redirect status: 301 (permanent), 302 (temporary), 307, 308. */
    @Column(name = "redirect_type", nullable = false)
    @Builder.Default
    private short redirectType = 302;

    /** Thời điểm bắt đầu hoạt động (null = ngay lập tức). */
    @Column(name = "starts_at")
    private Instant startsAt;

    /** Thời điểm hết hạn (null = vĩnh viễn). */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Giới hạn tổng lượt click (null = không giới hạn). */
    @Column(name = "max_clicks")
    private Long maxClicks;

    /** Tổng lượt click — cập nhật bất đồng bộ từ analytics. */
    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private long clickCount = 0;

    @Column(name = "last_clicked_at")
    private Instant lastClickedAt;

    /** BCrypt hash mật khẩu — nếu != null, link yêu cầu nhập mật khẩu. */
    @Column(name = "password_hash")
    private String passwordHash;

    /** Dữ liệu mở rộng (JSONB): UTM params, custom fields, ... */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "created_by_ip_hash")
    private byte[] createdByIpHash;

    @Column(name = "created_by_user_agent_hash")
    private byte[] createdByUserAgentHash;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Tags gắn vào link — quan hệ M:N qua bảng link.link_tags. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "link_tags", schema = "link", joinColumns = @JoinColumn(name = "link_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Kiểm tra link có yêu cầu mật khẩu không. */
    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}
