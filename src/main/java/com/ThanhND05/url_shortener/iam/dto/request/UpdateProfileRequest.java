package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO cập nhật thông tin profile (tên hiển thị, avatar).
 * Chỉ các trường có giá trị (non-null) mới được cập nhật.
 */
public record UpdateProfileRequest(
        @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
        String displayName,
        String avatarUrl
) {}
