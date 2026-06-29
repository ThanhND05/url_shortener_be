package com.ThanhND05.url_shortener.iam.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Controller quản lý Role & Permission — chỉ dành cho admin.
 *
 * Endpoints:
 *   GET    /api/v1/roles                         → liệt kê tất cả roles.
 *   POST   /api/v1/roles                         → tạo custom role.
 *   PUT    /api/v1/roles/{id}/permissions         → cập nhật permissions cho role.
 *   POST   /api/v1/roles/users/{userId}/assign    → gán role cho user.
 *   GET    /api/v1/roles/users/{userId}/permissions → xem effective permissions của user.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class RoleController {

    private final RoleService roleService;

    /** Liệt kê tất cả roles kèm permissions. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getAllRoles()));
    }

    /** Tạo custom role mới với danh sách permission slugs. */
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @RequestParam String name,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String description,
            @RequestBody Set<String> permissionSlugs) {
        RoleResponse role = roleService.createRole(name, displayName, description, permissionSlugs);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(role));
    }

    /** Cập nhật toàn bộ permissions của role. */
    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> updatePermissions(
            @PathVariable Long id,
            @RequestBody Set<String> permissionSlugs) {
        return ResponseEntity.ok(ApiResponse.ok(
                roleService.updateRolePermissions(id, permissionSlugs)));
    }

    /** Gán role cho user. */
    @PostMapping("/users/{userId}/assign")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request) {
        UUID grantedBy = SecurityUtils.getCurrentUserId();
        roleService.assignRoleToUser(userId, request, grantedBy);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã gán role thành công."));
    }

    /** Xem effective permissions của user (debug/admin tool). */
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<ApiResponse<Set<String>>> getUserPermissions(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                roleService.getEffectivePermissions(userId)));
    }
}
