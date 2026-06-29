package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu đăng ký tài khoản mới.
 * Sử dụng Java record (immutable) + Bean Validation annotations.
 *
 * @param email       email đăng nhập (bắt buộc, phải đúng format).
 * @param password    mật khẩu (bắt buộc, tối thiểu 8 ký tự).
 * @param displayName tên hiển thị (tùy chọn).
 */
public record RegisterRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
        String password,

        String displayName
) {}
