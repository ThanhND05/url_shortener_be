package com.ThanhND05.url_shortener.platform.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.platform.dto.request.CreateBlockedDomainRequest;
import com.ThanhND05.url_shortener.platform.dto.response.BlockedDomainResponse;
import com.ThanhND05.url_shortener.platform.service.BlockedDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller blocked domains — chỉ admin.
 *
 *   GET    /api/v1/blocked-domains       → danh sách blocked domains.
 *   POST   /api/v1/blocked-domains       → thêm domain vào blacklist.
 *   DELETE /api/v1/blocked-domains/{id}  → xóa domain khỏi blacklist.
 */
@RestController
@RequestMapping("/api/v1/blocked-domains")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class BlockedDomainController {

    private final BlockedDomainService blockedDomainService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BlockedDomainResponse>>> list(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(blockedDomainService.listAll(pageable))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlockedDomainResponse>> add(
            @Valid @RequestBody CreateBlockedDomainRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(blockedDomainService.addBlockedDomain(request),
                        "Domain đã được thêm vào blacklist."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id) {
        blockedDomainService.remove(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Domain đã được xóa khỏi blacklist."));
    }
}
