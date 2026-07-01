package com.ThanhND05.url_shortener.billing.entity;

import com.ThanhND05.url_shortener.billing.enums.SubscriptionPlan;
import com.ThanhND05.url_shortener.billing.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng billing.subscriptions — gói dịch vụ hiện tại của user.
 *
 * Quan hệ 1-1 với iam.users (user_id là PK).
 * Mặc định: plan = FREE, status = ACTIVE, không có expires_at.
 * Khi thanh toán thành công: plan = PRO, expires_at = +30 ngày.
 *
 * links_used: đếm số link đã tạo trong tháng hiện tại.
 * links_reset_at: thời điểm reset counter (đầu mỗi tháng).
 */
@Entity
@Table(name = "subscriptions", schema = "billing")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    /** Thời điểm bắt đầu gói Pro (null nếu FREE). */
    @Column(name = "started_at")
    private Instant startedAt;

    /** Thời điểm hết hạn gói Pro (null nếu FREE). */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Số link đã tạo trong tháng hiện tại (reset mỗi tháng). */
    @Column(name = "links_used", nullable = false)
    @Builder.Default
    private int linksUsed = 0;

    /** Thời điểm reset counter links_used (đầu mỗi tháng). */
    @Column(name = "links_reset_at", nullable = false)
    private Instant linksResetAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Business Methods ────────────────────────────────

    /** Kiểm tra user đang dùng gói Pro và còn hiệu lực. */
    public boolean isPro() {
        return plan == SubscriptionPlan.PRO
                && status == SubscriptionStatus.ACTIVE
                && expiresAt != null
                && expiresAt.isAfter(Instant.now());
    }

    /** Upgrade lên Pro, có hiệu lực 30 ngày kể từ bây giờ. */
    public void upgradeToPro() {
        this.plan = SubscriptionPlan.PRO;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = Instant.now();
        // Nếu gói Pro cũ chưa hết hạn → gia hạn thêm 30 ngày từ ngày hết hạn
        if (this.expiresAt != null && this.expiresAt.isAfter(Instant.now())) {
            this.expiresAt = this.expiresAt.plusSeconds(30L * 24 * 3600);
        } else {
            this.expiresAt = Instant.now().plusSeconds(30L * 24 * 3600);
        }
    }

    /** Đánh dấu hết hạn → tự động hạ xuống FREE. */
    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.plan = SubscriptionPlan.FREE;
    }
}
