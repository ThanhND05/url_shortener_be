package com.ThanhND05.url_shortener.iam.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller quản lý API Keys — cho phép user tạo/liệt kê/thu hồi API keys.
 *
 * Endpoints (yêu cầu authenticated):
 *   POST   /api/v1/api-keys       → tạo API key mới (trả raw key lần duy nhất).
 *   GET    /api/v1/api-keys       → liệt kê API keys của tôi.
 *   DELETE /api/v1/api-keys/{id}  → thu hồi (revoke) API key.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Tạo API key mới.
     * ⚠️ Raw key CHỈ trả về trong response này — không thể xem lại.
     * Client phải lưu raw key ngay.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        ApiKeyResponse response = apiKeyService.createApiKey(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "API key tạo thành công. Hãy lưu lại key ngay!"));
    }

    /** Liệt kê tất cả API keys của user hiện tại (không hiển thị raw key). */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApiKeyResponse>>> listApiKeys(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(apiKeyService.listApiKeys(ownerId, pageable))));
    }

    /** Thu hồi API key — key sẽ không còn sử dụng được. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(@PathVariable UUID id) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        apiKeyService.revokeApiKey(id, ownerId);
        return ResponseEntity.ok(ApiResponse.ok(null, "API key đã bị thu hồi."));
    }
}
