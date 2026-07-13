package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO chỉnh sửa permission.
 * Partial update: chỉ cập nhật các trường non-null.
 *
 * Lưu ý: resource và action có thể thay đổi, nhưng cặp (resource, action) mới
 * phải là duy nhất trong hệ thống (unique constraint ở DB).
 *
 * @param resource    tên tài nguyên mới (tùy chọn).
 * @param action      hành động mới (tùy chọn).
 * @param description mô tả mới (tùy chọn).
 */
public record UpdatePermissionRequest(
        @Size(max = 80, message = "Resource tối đa 80 ký tự")
        String resource,

        @Size(max = 80, message = "Action tối đa 80 ký tự")
        String action,

        String description
) {}
