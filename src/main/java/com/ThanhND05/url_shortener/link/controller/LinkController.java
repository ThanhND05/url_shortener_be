package com.ThanhND05.url_shortener.link.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import com.ThanhND05.url_shortener.link.dto.request.*;
import com.ThanhND05.url_shortener.link.dto.response.*;
import com.ThanhND05.url_shortener.link.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý short links.
 *
 *   POST   /api/v1/links                   → tạo short link mới.
 *   GET    /api/v1/links                   → danh sách links của tôi (phân trang).
 *   GET    /api/v1/links/{publicId}        → xem chi tiết link.
 *   PUT    /api/v1/links/{publicId}        → cập nhật link.
 *   DELETE /api/v1/links/{publicId}        → xóa mềm link.
 *   POST   /api/v1/links/{publicId}/rules  → thêm routing rule.
 *   GET    /api/v1/links/{publicId}/rules  → xem rules của link.
 */
@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    @PreAuthorize("hasAuthority('link:create')")
    public ResponseEntity<ApiResponse<LinkResponse>> create(
            @Valid @RequestBody CreateLinkRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(linkService.createLink(ownerId, request),
                        "Short link tạo thành công."));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('link:read')")
    public ResponseEntity<ApiResponse<PageResponse<LinkResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(linkService.listLinks(ownerId, pageable))));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('link:read')")
    public ResponseEntity<ApiResponse<LinkResponse>> get(@PathVariable UUID publicId) {
        return ResponseEntity.ok(ApiResponse.ok(linkService.getLink(publicId)));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('link:update')")
    public ResponseEntity<ApiResponse<LinkResponse>> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody UpdateLinkRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                linkService.updateLink(publicId, request, ownerId)));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('link:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID publicId) {
        linkService.deleteLink(publicId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Link đã được xóa."));
    }

    @PostMapping("/{publicId}/rules")
    @PreAuthorize("hasAuthority('link:update')")
    public ResponseEntity<ApiResponse<LinkRuleResponse>> addRule(
            @PathVariable UUID publicId,
            @Valid @RequestBody CreateLinkRuleRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(linkService.addRule(publicId, request, ownerId)));
    }

    @GetMapping("/{publicId}/rules")
    @PreAuthorize("hasAuthority('link:read')")
    public ResponseEntity<ApiResponse<List<LinkRuleResponse>>> getRules(
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(ApiResponse.ok(linkService.getRules(publicId)));
    }
}
