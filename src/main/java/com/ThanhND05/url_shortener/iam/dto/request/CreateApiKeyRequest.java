package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * DTO tạo API key mới.
 *
 * @param name      tên mô tả cho key (VD: "Production API").
 * @param scopes    danh sách permission slugs key được phép dùng.
 * @param expiresAt thời điểm hết hạn (null = không hết hạn).
 */
public record CreateApiKeyRequest(
        @NotBlank(message = "Tên API key không được để trống")
        String name,
        String[] scopes,
        Instant expiresAt
) {}
