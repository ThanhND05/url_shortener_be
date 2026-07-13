package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO tạo permission mới.
 *
 * @param resource    tên tài nguyên (VD: "link", "analytics", "billing").
 * @param action      hành động (VD: "create", "read", "update", "delete", "manage").
 * @param description mô tả ngắn cho permission (tùy chọn).
 */
public record CreatePermissionRequest(
        @NotBlank(message = "Resource không được để trống")
        @Size(max = 80, message = "Resource tối đa 80 ký tự")
        String resource,

        @NotBlank(message = "Action không được để trống")
        @Size(max = 80, message = "Action tối đa 80 ký tự")
        String action,

        String description
) {}
