package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO để super_admin chỉnh sửa thông tin tài khoản user.
 * Chỉ các trường non-null mới được cập nhật (partial update).
 *
 * @param email       email mới (tùy chọn, phải đúng format nếu có).
 * @param displayName tên hiển thị mới (tùy chọn).
 * @param avatarUrl   URL avatar mới (tùy chọn).
 * @param status      trạng thái mới: "ACTIVE" / "LOCKED" (tùy chọn).
 */
public record AdminUpdateUserRequest(
        @Email(message = "Email không đúng định dạng")
        String email,

        @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
        String displayName,

        String avatarUrl,

        String status
) {}
