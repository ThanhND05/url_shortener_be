package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO đổi mật khẩu — yêu cầu cả mật khẩu cũ (xác thực) và mới.
 * Sau khi đổi, tất cả refresh tokens bị revoke + access tokens bị blacklist.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
        String newPassword
) {}
