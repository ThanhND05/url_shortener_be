package com.ThanhND05.url_shortener.iam.entity;

import com.ThanhND05.url_shortener.iam.enums.ApiKeyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng iam.api_keys — API key cho programmatic access.
 *
 * Cơ chế hoạt động:
 * 1. User tạo API key → server sinh raw key (VD: "sk_live_abc123...xyz").
 * 2. Server lưu: key_prefix = "sk_live_abc" (hiển thị UI), key_hash = SHA-256(raw).
 *    Raw key CHỈ hiển thị MỘT LẦN lúc tạo, sau đó không thể xem lại.
 * 3. Khi client gọi API với header "X-API-Key: sk_live_abc123...xyz":
 *    - Server hash value → tìm trong DB theo key_hash.
 *    - Check status = ACTIVE, chưa hết hạn, scopes phù hợp.
 *
 * Scopes: mảng TEXT[] lưu danh sách quyền, VD: ['link:create', 'analytics:read'].
 * Dùng TEXT[] thay vì FK tới permissions để check nhanh mà không cần JOIN.
 * Rate limit: giới hạn số request/phút cho mỗi key.
 */
@Entity
@Table(name = "api_keys", schema = "iam")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK tới user sở hữu key. */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** Tên mô tả key (do user đặt), VD: "Production API", "CI/CD Pipeline". */
    @Column(nullable = false, length = 150)
    private String name;

    /** Prefix hiển thị trên UI: "sk_live_abc..." — giúp user nhận diện key. */
    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    /** SHA-256 hash của raw API key — dùng để lookup khi authenticate. */
    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    /**
     * Danh sách permission slugs: ["link:create", "analytics:read"].
     * Lưu dạng TEXT[] trong PostgreSQL.
     * Dùng columnDefinition vì Hibernate cần biết kiểu cột chính xác.
     */
    @Column(name = "scopes", columnDefinition = "text[]")
    @Builder.Default
    private String[] scopes = {};

    /** Giới hạn request/phút cho key này. */
    @Column(name = "rate_limit_per_minute", nullable = false)
    @Builder.Default
    private int rateLimitPerMinute = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    @Column(name = "revoke_reason")
    private String revokeReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
