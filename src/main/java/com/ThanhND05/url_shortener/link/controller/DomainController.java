package com.ThanhND05.url_shortener.link.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import com.ThanhND05.url_shortener.link.dto.request.CreateDomainRequest;
import com.ThanhND05.url_shortener.link.dto.response.DomainResponse;
import com.ThanhND05.url_shortener.link.service.DomainService;
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
 * Controller quản lý custom domains.
 *
 *   POST   /api/v1/domains               → đăng ký domain mới.
 *   GET    /api/v1/domains               → liệt kê domains của tôi.
 *   PUT    /api/v1/domains/{id}/verify   → xác minh domain.
 *   PUT    /api/v1/domains/{id}/default  → đặt làm domain mặc định.
 *   DELETE /api/v1/domains/{id}          → xóa mềm domain.
 */
@RestController
@RequestMapping("/api/v1/domains")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('domain:create') or hasAuthority('domain:read')")
public class DomainController {

    private final DomainService domainService;

    @PostMapping
    @PreAuthorize("hasAuthority('domain:create')")
    public ResponseEntity<ApiResponse<DomainResponse>> create(
            @Valid @RequestBody CreateDomainRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(domainService.createDomain(ownerId, request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('domain:read')")
    public ResponseEntity<ApiResponse<PageResponse<DomainResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(domainService.listDomains(ownerId, pageable))));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<DomainResponse>> verify(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                domainService.verifyDomain(id, SecurityUtils.getCurrentUserId()),
                "Domain đã được xác minh."));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<DomainResponse>> setDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                domainService.setDefault(id, SecurityUtils.getCurrentUserId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('domain:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        domainService.deleteDomain(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Domain đã được xóa."));
    }
}
