package com.ThanhND05.url_shortener.link.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.link.dto.request.AdminUpdateLinkStatusRequest;
import com.ThanhND05.url_shortener.link.dto.response.AdminLinkResponse;
import com.ThanhND05.url_shortener.link.service.AdminLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Controller admin quản lý links toàn hệ thống.
 * Tất cả endpoints yêu cầu permission "user:manage" (Super Admin / Admin).
 *
 * Endpoints:
 *   GET    /api/v1/admin/links                      → tìm kiếm/filter links.
 *   GET    /api/v1/admin/links/{publicId}           → chi tiết link bất kỳ.
 *   PUT    /api/v1/admin/links/{publicId}/status     → đổi trạng thái (ban/unban).
 *   PUT    /api/v1/admin/links/{publicId}/redirect   → đổi redirect type (301/302/307/308).
 *   DELETE /api/v1/admin/links/{publicId}           → xóa mềm link.
 */
@RestController
@RequestMapping("/api/v1/admin/links")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class AdminLinkController {

    private final AdminLinkService adminLinkService;

    /**
     * Tìm kiếm/filter links toàn hệ thống.
     *
     * @param search   tìm theo URL gốc hoặc short code (ILIKE).
     * @param status   filter theo trạng thái: ACTIVE, DISABLED, QUARANTINED, EXPIRED, DELETED.
     * @param ownerId  filter theo user tạo.
     * @param from     filter từ ngày tạo (ISO-8601).
     * @param to       filter đến ngày tạo (ISO-8601).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminLinkResponse>>> searchLinks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(adminLinkService.searchLinks(
                        search, status, ownerId, from, to, pageable))));
    }

    /** Xem chi tiết link bất kỳ (bao gồm thông tin owner). */
    @GetMapping("/{publicId}")
    public ResponseEntity<ApiResponse<AdminLinkResponse>> getLink(@PathVariable UUID publicId) {
        return ResponseEntity.ok(ApiResponse.ok(adminLinkService.getLink(publicId)));
    }

    /**
     * Đổi trạng thái link (ban/unban/quarantine).
     * VD: Admin phát hiện link phishing → PUT status = QUARANTINED.
     */
    @PutMapping("/{publicId}/status")
    public ResponseEntity<ApiResponse<AdminLinkResponse>> updateStatus(
            @PathVariable UUID publicId,
            @Valid @RequestBody AdminUpdateLinkStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminLinkService.updateLinkStatus(publicId, request),
                "Cập nhật trạng thái link thành công."));
    }

    /**
     * Đổi redirect type (301 Permanent / 302 Temporary / 307 / 308).
     */
    @PutMapping("/{publicId}/redirect")
    public ResponseEntity<ApiResponse<AdminLinkResponse>> updateRedirectType(
            @PathVariable UUID publicId,
            @RequestParam short redirectType) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminLinkService.updateRedirectType(publicId, redirectType),
                "Cập nhật redirect type thành công."));
    }

    /** Xóa mềm link bất kỳ. */
    @DeleteMapping("/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteLink(@PathVariable UUID publicId) {
        adminLinkService.deleteLink(publicId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Link đã được xóa."));
    }
}
