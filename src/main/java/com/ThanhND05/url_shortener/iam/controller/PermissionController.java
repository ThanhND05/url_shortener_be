package com.ThanhND05.url_shortener.iam.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý Permission — chỉ dành cho admin (user:manage).
 *
 * Endpoints:
 *   GET    /api/v1/permissions           → liệt kê tất cả permissions.
 *   GET    /api/v1/permissions/{id}      → xem chi tiết permission.
 *   POST   /api/v1/permissions           → tạo permission mới.
 *   PUT    /api/v1/permissions/{id}      → chỉnh sửa permission.
 *   DELETE /api/v1/permissions/{id}      → xóa permission.
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class PermissionController {

    private final PermissionService permissionService;

    /** Liệt kê tất cả permissions. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.getAllPermissions()));
    }

    /** Xem chi tiết permission. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.getPermissionById(id)));
    }

    /** Tạo permission mới. */
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        PermissionResponse created = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Tạo permission thành công."));
    }

    /** Chỉnh sửa permission. */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                permissionService.updatePermission(id, request),
                "Cập nhật permission thành công."));
    }

    /** Xóa permission. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa permission thành công."));
    }
}
