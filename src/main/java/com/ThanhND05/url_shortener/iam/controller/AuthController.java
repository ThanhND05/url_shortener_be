package com.ThanhND05.url_shortener.iam.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.service.AuthService;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý xác thực — tất cả endpoint ở đây đều PUBLIC (không yêu cầu JWT).
 *
 * Endpoints:
 *   POST /api/v1/auth/register  → đăng ký tài khoản mới.
 *   POST /api/v1/auth/login     → đăng nhập, nhận access + refresh token.
 *   POST /api/v1/auth/refresh   → dùng refresh token lấy cặp token mới.
 *   POST /api/v1/auth/logout    → revoke refresh token hiện tại (cần auth).
 *   POST /api/v1/auth/logout-all → revoke tất cả sessions (cần auth).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * ĐĂNG KÝ — tạo tài khoản mới.
     * Tự động gán role "member" và trả về cặp token để client đăng nhập ngay.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Đăng ký thành công."));
    }

    /**
     * ĐĂNG NHẬP — xác thực bằng email + mật khẩu.
     * Trả về JWT access token (15 phút) và refresh token (7 ngày).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập thành công."));
    }

    /**
     * REFRESH — đổi refresh token cũ lấy cặp token mới.
     * Token cũ bị revoke ngay, token mới kế thừa family_id (phát hiện reuse).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * LOGOUT — revoke refresh token + blacklist access token hiện tại (một device).
     * Yêu cầu authenticated (gửi access token trong header).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String accessToken = extractBearerToken(httpRequest);
        authService.logout(request.refreshToken(), accessToken);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đăng xuất thành công."));
    }

    /**
     * LOGOUT TẤT CẢ — revoke toàn bộ refresh tokens + blacklist access token (tất cả devices).
     * Yêu cầu authenticated.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(HttpServletRequest httpRequest) {
        String accessToken = extractBearerToken(httpRequest);
        authService.logoutAll(SecurityUtils.getCurrentUserId(), accessToken);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã đăng xuất tất cả thiết bị."));
    }

    // ── PRIVATE HELPERS ─────────────────────────────────

    /**
     * Trích xuất Bearer token từ header Authorization.
     * Trả về raw JWT string (không có prefix "Bearer ").
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
