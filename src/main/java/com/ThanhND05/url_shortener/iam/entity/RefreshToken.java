package com.ThanhND05.url_shortener.iam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng iam.refresh_tokens — lưu refresh token đã hash.
 *
 * Cơ chế hoạt động (Refresh Token Rotation):
 * 1. Khi user đăng nhập, server tạo một refresh token mới, hash nó (SHA-256),
 *    lưu hash vào DB, trả raw token cho client.
 * 2. Khi client dùng refresh token để lấy access token mới:
 *    - Server hash token client gửi, tìm trong DB.
 *    - Nếu hợp lệ + chưa revoke + chưa hết hạn → tạo access token mới
 *      + tạo refresh token mới (cùng family_id) + revoke token cũ.
 * 3. Family rotation: nếu phát hiện token cũ trong family bị dùng lại
 *    → nghi ngờ token bị đánh cắp → revoke TOÀN BỘ family (tất cả token
 *    có cùng family_id) → user phải đăng nhập lại.
 *
 * Các trường quan trọng:
 * - family_id: nhóm các token cùng chuỗi rotation, detect token theft.
 * - session_id: nhóm theo thiết bị, cho phép logout một device cụ thể.
 * - ip_hash/user_agent_hash: fingerprint thiết bị, dùng để phát hiện bất thường.
 */
@Entity
@Table(name = "refresh_tokens", schema = "iam")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK tới user sở hữu token này. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** SHA-256 hash của raw refresh token — raw token chỉ client biết. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /**
     * ID nhóm rotation — tất cả token sinh ra từ cùng một lần đăng nhập
     * chia sẻ family_id. Nếu token cũ trong family bị reuse → revoke cả family.
     */
    @Column(name = "family_id", nullable = false)
    @Builder.Default
    private UUID familyId = UUID.randomUUID();

    /** ID phiên đăng nhập — gom theo thiết bị để hỗ trợ "logout device X". */
    @Column(name = "session_id", nullable = false)
    @Builder.Default
    private UUID sessionId = UUID.randomUUID();

    /** Tên thiết bị (tùy chọn, lấy từ User-Agent parse). */
    @Column(name = "device_name", length = 150)
    private String deviceName;

    /** Hash IP đăng nhập (BYTEA) — dùng phát hiện bất thường. */
    @Column(name = "ip_hash")
    private byte[] ipHash;

    /** Hash User-Agent (BYTEA). */
    @Column(name = "user_agent_hash")
    private byte[] userAgentHash;

    /** Thời điểm hết hạn — quá thời gian này token không còn sử dụng được. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Thời điểm bị thu hồi (null = chưa revoke, vẫn hợp lệ). */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Lý do revoke: LOGOUT / PASSWORD_CHANGE / SUSPICIOUS / FAMILY_REUSE. */
    @Column(name = "revoke_reason", length = 80)
    private String revokeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Kiểm tra token còn hiệu lực: chưa revoke + chưa hết hạn. */
    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    /** Revoke token với lý do cụ thể. */
    public void revoke(String reason) {
        this.revokedAt = Instant.now();
        this.revokeReason = reason;
    }
}
