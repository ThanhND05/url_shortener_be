package com.ThanhND05.url_shortener.platform.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.platform.dto.request.UpdateSystemConfigRequest;
import com.ThanhND05.url_shortener.platform.dto.response.SystemConfigResponse;
import com.ThanhND05.url_shortener.platform.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller system configs — chỉ admin.
 *
 *   GET    /api/v1/system-configs              → danh sách toàn bộ config.
 *   GET    /api/v1/system-configs/{key}        → chi tiết 1 config.
 *   PUT    /api/v1/system-configs/{key}        → cập nhật giá trị config.
 *   DELETE /api/v1/system-configs/{key}        → xóa config (cẩn thận!).
 */
@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.listAll()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigService.getByKey(key)));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> update(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        return ResponseEntity.ok(ApiResponse.ok(
                systemConfigService.updateConfig(key, request, actorEmail),
                "Cấu hình đã được cập nhật thành công."));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String key) {
        systemConfigService.deleteConfig(key);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cấu hình đã được xóa."));
    }
}
