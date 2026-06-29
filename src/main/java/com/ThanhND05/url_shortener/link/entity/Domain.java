package com.ThanhND05.url_shortener.link.entity;

import com.ThanhND05.url_shortener.link.enums.DomainStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng link.domains — custom domain để tạo short URL.
 *
 * Cơ chế hoạt động:
 * - Mỗi user có thể đăng ký nhiều custom domain (VD: "short.mysite.com").
 * - Domain cần được xác minh quyền sở hữu trước khi dùng (verification_token + DNS check).
 * - Mỗi user chỉ có đúng 1 domain mặc định (is_default = true, enforced bởi unique partial index).
 * - Khi tạo short link, user chọn domain hoặc dùng domain mặc định.
 * - Status flow: PENDING → ACTIVE (sau verify) → BLOCKED/DELETED.
 *
 * Ví dụ: User đăng ký domain "go.myshop.vn"
 *   → Server sinh verification_token = "verify_abc123"
 *   → User tạo DNS TXT record: _url-verify.go.myshop.vn = verify_abc123
 *   → Gọi API verify → server check DNS → set status = ACTIVE, verified_at = now()
 */
@Entity
@Table(name = "domains", schema = "link")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public UUID — dùng trong API thay vì expose internal BIGINT id. */
    @Column(name = "public_id", nullable = false, unique = true)
    @Builder.Default
    private UUID publicId = UUID.randomUUID();

    /** UUID của user sở hữu domain. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Tên domain (case-insensitive nhờ CITEXT). VD: "go.myshop.vn". */
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String domain;

    /**
     * Đánh dấu domain mặc định của user.
     * Unique partial index đảm bảo mỗi owner chỉ có 1 domain default.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DomainStatus status = DomainStatus.PENDING;

    /** Token xác minh — user cần tạo DNS TXT record chứa token này. */
    @Column(name = "verification_token")
    private String verificationToken;

    /** Thời điểm xác minh thành công. */
    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
