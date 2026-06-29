package com.ThanhND05.url_shortener.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho bảng platform.idempotency_keys — chống duplicate request.
 *
 * Cơ chế hoạt động:
 * - Client gửi header `Idempotency-Key: <unique-key>` cùng mutating request (POST/PUT/DELETE).
 * - Server check: key đã tồn tại → trả cached response (không xử lý lại).
 * - Key chưa tồn tại → xử lý request → lưu key + response → trả kết quả.
 * - Key có TTL (expires_at) → tự dọn bởi scheduled cleanup job.
 *
 * Tại sao cần idempotency?
 * - Network retry: client gửi lại request khi timeout → server không tạo duplicate.
 * - Payment safety: tránh charge 2 lần.
 * - API key creation: tránh tạo 2 key giống nhau.
 *
 * request_hash: SHA-256(request body) — detect nếu client gửi key cũ nhưng body khác (lỗi).
 */
@Entity
@Table(name = "idempotency_keys", schema = "platform")
@IdClass(IdempotencyKeyId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @Column(name = "owner_id")
    private UUID ownerId;

    @Id
    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    /** SHA-256 hash request body — validate key + body match. */
    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    /** HTTP status code của response đã cache. */
    @Column(name = "response_status")
    private Integer responseStatus;

    /** Response body đã cache (JSONB). */
    @Column(name = "response_body", columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Sau thời điểm này, key có thể bị xóa. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
