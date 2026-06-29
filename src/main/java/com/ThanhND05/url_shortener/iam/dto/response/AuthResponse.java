package com.ThanhND05.url_shortener.iam.dto.response;

import lombok.Builder;

/**
 * DTO phản hồi sau khi đăng nhập / refresh token thành công.
 * Chứa cả access token (JWT) và refresh token (opaque string).
 *
 * @param accessToken  JWT access token — client gửi trong header "Authorization: Bearer {token}".
 * @param refreshToken opaque refresh token — client gửi khi cần lấy access token mới.
 * @param tokenType    luôn là "Bearer".
 * @param expiresIn    thời gian sống của access token (giây).
 * @param user         thông tin cơ bản của user (để client hiển thị ngay, không cần gọi /me).
 */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public AuthResponse {
        if (tokenType == null) tokenType = "Bearer";
    }
}
