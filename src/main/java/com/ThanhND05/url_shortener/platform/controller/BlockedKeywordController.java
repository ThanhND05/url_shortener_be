package com.ThanhND05.url_shortener.platform.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.dto.PageResponse;
import com.ThanhND05.url_shortener.platform.dto.request.CreateBlockedKeywordRequest;
import com.ThanhND05.url_shortener.platform.dto.response.BlockedKeywordResponse;
import com.ThanhND05.url_shortener.platform.service.BlockedKeywordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller blocked keywords — chỉ admin.
 *
 *   GET    /api/v1/blocked-keywords       → danh sách blocked keywords (có search).
 *   POST   /api/v1/blocked-keywords       → thêm keyword vào danh sách cấm.
 *   DELETE /api/v1/blocked-keywords/{id}  → xóa keyword khỏi danh sách cấm.
 */
@RestController
@RequestMapping("/api/v1/blocked-keywords")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class BlockedKeywordController {

    private final BlockedKeywordService blockedKeywordService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BlockedKeywordResponse>>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(blockedKeywordService.listAll(search, pageable))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlockedKeywordResponse>> add(
            @Valid @RequestBody CreateBlockedKeywordRequest request,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        blockedKeywordService.addKeyword(request, actorEmail),
                        "Keyword đã được thêm vào danh sách cấm."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id) {
        blockedKeywordService.remove(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Keyword đã được xóa khỏi danh sách cấm."));
    }
}
