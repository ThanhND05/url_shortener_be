package com.ThanhND05.url_shortener.link.controller;

import com.ThanhND05.url_shortener.common.dto.ApiResponse;
import com.ThanhND05.url_shortener.common.security.SecurityUtils;
import com.ThanhND05.url_shortener.link.dto.request.CreateTagRequest;
import com.ThanhND05.url_shortener.link.dto.response.TagResponse;
import com.ThanhND05.url_shortener.link.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý tags.
 *
 *   POST   /api/v1/tags       → tạo tag mới.
 *   GET    /api/v1/tags       → liệt kê tags của tôi.
 *   DELETE /api/v1/tags/{id}  → xóa tag.
 */
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> create(
            @Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tagService.createTag(
                        SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(
                tagService.listTags(SecurityUtils.getCurrentUserId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tagService.deleteTag(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Tag đã được xóa."));
    }
}
