package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO để super_admin tạo tài khoản user mới.
 *
 * @param email       email đăng nhập (bắt buộc, phải đúng format).
 * @param password    mật khẩu ban đầu (bắt buộc, tối thiểu 8 ký tự).
 * @param displayName tên hiển thị (tùy chọn).
 * @param roleName    tên role cần gán ngay (tùy chọn, VD: "admin", "member").
 *                    Nếu null → mặc định gán role "member".
 */
public record AdminCreateUserRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
        String password,

        @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
        String displayName,

        String roleName
) {}
