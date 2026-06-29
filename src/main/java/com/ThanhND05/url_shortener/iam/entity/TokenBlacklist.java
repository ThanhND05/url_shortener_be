package com.ThanhND05.url_shortener.iam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng iam.token_blacklist — danh sách đen access token.
 *
 * Cơ chế hoạt động:
 * - Access token (JWT) là stateless — server không lưu trạng thái.
 * - Khi cần vô hiệu hóa KHẨN CẤP một access token đang còn hạn
 *   (VD: user đổi mật khẩu, bị khóa tài khoản), server lưu JTI (JWT ID)
 *   của token đó vào bảng blacklist.
 * - Khi có sự kiện bảo mật, server kiểm tra JTI trong blacklist.
 *   KHÔNG kiểm tra trên MỌI request (quá tốn performance).
 * - Mỗi entry có expires_at = thời gian hết hạn của access token gốc.
 *   Sau khi token hết hạn tự nhiên, entry được cleanup job xóa đi.
 */
@Entity
@Table(name = "token_blacklist", schema = "iam")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    /** JTI claim trong JWT — là UUID unique cho mỗi access token. */
    @Id
    @Column(name = "jti")
    private UUID jti;

    /** User sở hữu access token bị blacklist. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Thời gian hết hạn của access token gốc — dùng cho cleanup job. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Lý do blacklist: PASSWORD_CHANGE / ACCOUNT_LOCKED / SUSPICIOUS. */
    @Column(name = "reason", length = 80)
    private String reason;
}
