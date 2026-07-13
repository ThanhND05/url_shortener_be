package com.ThanhND05.url_shortener.iam.dto.response;

import com.ThanhND05.url_shortener.iam.entity.Permission;
import lombok.Builder;

import java.time.Instant;

/**
 * DTO phản hồi thông tin permission.
 * Trả về slug (resource:action) cùng description để admin dễ quản lý.
 */
@Builder
public record PermissionResponse(
        Long id,
        String resource,
        String action,
        String slug,
        String description,
        Instant createdAt
) {
    /** Convert từ Permission entity sang DTO. */
    public static PermissionResponse from(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .resource(permission.getResource())
                .action(permission.getAction())
                .slug(permission.toSlug())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
