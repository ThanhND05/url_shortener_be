package com.ThanhND05.url_shortener.platform.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.platform.dto.response.AuditLogResponse;
import com.ThanhND05.url_shortener.platform.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller audit logs — chỉ admin.
 *
 *   GET /api/v1/audit-logs              → tất cả audit logs (phân trang).
 *   GET /api/v1/audit-logs/users/{id}   → logs theo user.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> listAll(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(auditService.listAll(pageable))));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> listByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(auditService.listByActor(userId, pageable))));
    }
}
