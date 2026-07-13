package com.ThanhND05.url_shortener.iam.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller quản lý thông tin user.
 *
 * Endpoints cho user thường (authenticated):
 *   GET    /api/v1/users/me           → xem profile.
 *   PUT    /api/v1/users/me           → cập nhật profile.
 *   PUT    /api/v1/users/me/password  → đổi mật khẩu.
 *
 * Endpoints cho admin (cần permission "user:manage"):
 *   POST   /api/v1/users              → tạo tài khoản user mới.
 *   GET    /api/v1/users              → danh sách user (phân trang).
 *   GET    /api/v1/users/{id}         → xem chi tiết user.
 *   PUT    /api/v1/users/{id}         → chỉnh sửa thông tin user.
 *   PUT    /api/v1/users/{id}/lock    → khóa tài khoản.
 *   PUT    /api/v1/users/{id}/unlock  → mở khóa.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── USER: Profile ─────────────────────────────────────

    /** Xem thông tin profile hiện tại — lấy userId từ JWT. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(userId)));
    }

    /** Cập nhật profile (tên hiển thị, avatar). */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                userService.updateProfile(userId, request), "Cập nhật thành công."));
    }

    /** Đổi mật khẩu — yêu cầu nhập mật khẩu cũ + mới. Revoke tất cả sessions. */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    // ── ADMIN: Quản lý user ───────────────────────────────

    /** Tạo tài khoản user mới — chỉ super_admin. */
    @PostMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Tạo tài khoản thành công."));
    }

    /** Liệt kê tất cả user (phân trang) — chỉ admin. */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(userService.listUsers(pageable))));
    }

    /** Xem chi tiết thông tin user — chỉ admin. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    /** Chỉnh sửa thông tin user — chỉ super_admin. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.adminUpdateUser(id, request), "Cập nhật thông tin user thành công."));
    }

    /** Khóa tài khoản — chỉ admin có quyền "user:manage". */
    @PutMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.lockUser(id), "Đã khóa tài khoản."));
    }

    /** Mở khóa tài khoản. */
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.unlockUser(id), "Đã mở khóa tài khoản."));
    }
}
