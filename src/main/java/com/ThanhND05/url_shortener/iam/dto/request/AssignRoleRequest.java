package com.ThanhND05.url_shortener.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO gán role cho user.
 *
 * @param roleName  tên role slug (VD: "admin", "member").
 * @param scopeType "GLOBAL" hoặc "WORKSPACE".
 * @param scopeId   UUID workspace (null nếu GLOBAL).
 */
public record AssignRoleRequest(
        @NotBlank(message = "Tên role không được để trống")
        String roleName,

        @NotNull(message = "Scope type không được để trống")
        String scopeType,

        UUID scopeId
) {}
