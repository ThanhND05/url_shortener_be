package com.ThanhND05.url_shortener.platform.service;

import com.ThanhND05.url_shortener.common.exception.DuplicateResourceException;
import com.ThanhND05.url_shortener.common.exception.ResourceNotFoundException;
import com.ThanhND05.url_shortener.platform.dto.request.CreateBlockedKeywordRequest;
import com.ThanhND05.url_shortener.platform.dto.response.BlockedKeywordResponse;
import com.ThanhND05.url_shortener.platform.entity.BlockedKeyword;
import com.ThanhND05.url_shortener.platform.repository.BlockedKeywordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý blocked keywords — từ khóa bị cấm trong slug/URL.
 *
 * Cross-module interface:
 * - Link module gọi containsBlockedKeyword(text) trước khi tạo short link.
 * - Nếu slug/URL chứa blocked keyword → reject.
 *
 * Tối ưu hóa:
 * - Cache toàn bộ keyword trong ConcurrentHashSet tại memory.
 * - Reload cache khi thêm/xóa keyword.
 * - Tránh query DB mỗi lần tạo link.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockedKeywordService {

    private final BlockedKeywordRepository blockedKeywordRepository;

    /** In-memory cache các keyword đã lowercase để so sánh nhanh. */
    private final Set<String> keywordCache = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void initCache() {
        reloadCache();
        log.info("Loaded {} blocked keywords into cache.", keywordCache.size());
    }

    private void reloadCache() {
        keywordCache.clear();
        List<String> keywords = blockedKeywordRepository.findAllKeywords();
        keywords.forEach(k -> keywordCache.add(k.toLowerCase()));
    }

    // ── Public API (Cross-module) ────────────────────────

    /**
     * Kiểm tra text (slug hoặc URL) có chứa blocked keyword không.
     * Sử dụng in-memory cache, O(n) với n = số keyword (thường rất nhỏ < 1000).
     *
     * @param text chuỗi cần kiểm tra.
     * @return true nếu chứa từ khóa bị cấm.
     */
    public boolean containsBlockedKeyword(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return keywordCache.stream().anyMatch(lower::contains);
    }

    /**
     * Tìm keyword bị vi phạm trong text (trả về keyword đầu tiên tìm thấy).
     *
     * @param text chuỗi cần kiểm tra.
     * @return keyword vi phạm hoặc null.
     */
    public String findViolatingKeyword(String text) {
        if (text == null || text.isBlank()) return null;
        String lower = text.toLowerCase();
        return keywordCache.stream()
                .filter(lower::contains)
                .findFirst()
                .orElse(null);
    }

    // ── Admin CRUD ───────────────────────────────────────

    @Transactional
    public BlockedKeywordResponse addKeyword(CreateBlockedKeywordRequest request, String actorEmail) {
        String keyword = request.keyword().toLowerCase().trim();

        if (blockedKeywordRepository.existsByKeyword(keyword)) {
            throw new DuplicateResourceException("BlockedKeyword", "keyword", keyword);
        }

        BlockedKeyword bk = BlockedKeyword.builder()
                .keyword(keyword)
                .reason(request.reason())
                .createdBy(actorEmail)
                .build();
        bk = blockedKeywordRepository.save(bk);

        // Update cache
        keywordCache.add(keyword);

        log.info("Blocked keyword added: '{}' by {}", keyword, actorEmail);
        return BlockedKeywordResponse.from(bk);
    }

    @Transactional(readOnly = true)
    public Page<BlockedKeywordResponse> listAll(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return blockedKeywordRepository.searchByKeyword(search.trim(), pageable)
                    .map(BlockedKeywordResponse::from);
        }
        return blockedKeywordRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(BlockedKeywordResponse::from);
    }

    @Transactional
    public void remove(Long id) {
        BlockedKeyword bk = blockedKeywordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlockedKeyword", "id", id));

        blockedKeywordRepository.delete(bk);

        // Update cache
        keywordCache.remove(bk.getKeyword().toLowerCase());

        log.info("Blocked keyword removed: '{}'", bk.getKeyword());
    }
}
