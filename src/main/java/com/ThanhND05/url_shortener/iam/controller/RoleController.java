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
 * Controller quản lý Role — chỉ dành cho admin.
 * (Permission CRUD đã tách riêng sang PermissionController.)
 *
 * Endpoints:
 *   GET    /api/v1/roles                         → liệt kê tất cả roles.
 *   GET    /api/v1/roles/{id}                    → xem chi tiết role.
 *   POST   /api/v1/roles                         → tạo custom role.
 *   PUT    /api/v1/roles/{id}                    → chỉnh sửa thông tin role.
 *   PUT    /api/v1/roles/{id}/permissions         → cập nhật permissions cho role.
 *   DELETE /api/v1/roles/{id}                    → xóa role (hỗ trợ ?force=true).
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

    /** Xem chi tiết role. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getRoleById(id)));
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

    /** Chỉnh sửa thông tin role (name, displayName, description). */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                roleService.updateRole(id, request),
                "Cập nhật role thành công."));
    }

    /** Cập nhật toàn bộ permissions của role. */
    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> updatePermissions(
            @PathVariable Long id,
            @RequestBody Set<String> permissionSlugs) {
        return ResponseEntity.ok(ApiResponse.ok(
                roleService.updateRolePermissions(id, permissionSlugs)));
    }

    /**
     * Xóa role — chỉ custom roles (is_system = false).
     * Nếu role đang gán cho users, cần ?force=true để xóa.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        roleService.deleteRole(id, force);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa role thành công."));
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

