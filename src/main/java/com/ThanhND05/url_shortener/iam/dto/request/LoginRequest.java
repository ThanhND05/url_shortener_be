package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu đăng nhập.
 *
 * @param email    email đăng nhập.
 * @param password mật khẩu plain text (server sẽ verify bằng BCrypt).
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {}
