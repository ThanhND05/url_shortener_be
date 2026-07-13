package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO chỉnh sửa thông tin role (không bao gồm permissions).
 * Partial update: chỉ cập nhật trường non-null.
 *
 * Lưu ý: System roles không được phép đổi tên (name).
 *
 * @param name        slug mới cho role (tùy chọn, chỉ custom roles).
 * @param displayName tên hiển thị mới (tùy chọn).
 * @param description mô tả mới (tùy chọn).
 */
public record UpdateRoleRequest(
        @Size(max = 80, message = "Tên role tối đa 80 ký tự")
        String name,

        @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
        String displayName,

        String description
) {}
