package com.ThanhND05.url_shortener.iam.dto.response;

import com.ThanhND05.url_shortener.iam.entity.Role;
import lombok.Builder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO phản hồi thông tin role kèm danh sách permission slugs.
 */
@Builder
public record RoleResponse(
        Long id,
        String name,
        String displayName,
        String description,
        boolean isSystem,
        Set<String> permissions
) {
    public static RoleResponse from(Role role) {
        Set<String> perms = role.getPermissions().stream()
                .map(p -> p.toSlug())
                .collect(Collectors.toSet());
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .isSystem(role.isSystem())
                .permissions(perms)
                .build();
    }
}
