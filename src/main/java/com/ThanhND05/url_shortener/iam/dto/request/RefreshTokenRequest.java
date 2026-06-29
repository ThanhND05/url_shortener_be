package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu refresh access token.
 * Client gửi refresh token đã nhận trước đó để lấy cặp token mới.
 *
 * @param refreshToken refresh token string (raw, chưa hash).
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken
) {}
