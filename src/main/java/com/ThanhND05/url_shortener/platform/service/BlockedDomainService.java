package com.ThanhND05.url_shortener.platform.service;

import com.ThanhND05.url_shortener.common.exception.DuplicateResourceException;
import com.ThanhND05.url_shortener.common.exception.ResourceNotFoundException;
import com.ThanhND05.url_shortener.platform.dto.request.CreateBlockedDomainRequest;
import com.ThanhND05.url_shortener.platform.dto.response.BlockedDomainResponse;
import com.ThanhND05.url_shortener.platform.entity.BlockedDomain;
import com.ThanhND05.url_shortener.platform.repository.BlockedDomainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/**
 * Service quản lý blocked domains — blacklist domain phishing/malware.
 *
 * Cross-module interface:
 * - Link module gọi isBlocked(url) trước khi tạo short link.
 * - Nếu original_url chứa blocked domain → reject với lỗi rõ ràng.
 *
 * Ví dụ:
 * - Blocked: "evil-phishing.com"
 * - User tạo link tới "https://evil-phishing.com/login" → reject
 * - User tạo link tới "https://google.com" → OK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockedDomainService {

    private final BlockedDomainRepository blockedDomainRepository;

    /**
     * Kiểm tra URL có chứa blocked domain không.
     * Link module gọi method này trước khi tạo short link.
     *
     * @param url original URL cần check.
     * @return true nếu domain bị chặn.
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return false;
            // Loại bỏ "www." prefix
            String domain = host.startsWith("www.") ? host.substring(4) : host;
            return blockedDomainRepository.existsByDomain(domain);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public BlockedDomainResponse addBlockedDomain(CreateBlockedDomainRequest request) {
        String domain = request.domain().toLowerCase().trim();
        if (domain.startsWith("www.")) domain = domain.substring(4);

        if (blockedDomainRepository.existsByDomain(domain)) {
            throw new DuplicateResourceException("BlockedDomain", "domain", domain);
        }

        BlockedDomain bd = BlockedDomain.builder()
                .domain(domain)
                .reason(request.reason())
                .source(request.source() != null ? request.source() : "manual")
                .build();
        bd = blockedDomainRepository.save(bd);
        log.info("Blocked domain added: {}", domain);
        return BlockedDomainResponse.from(bd);
    }

    @Transactional(readOnly = true)
    public Page<BlockedDomainResponse> listAll(Pageable pageable) {
        return blockedDomainRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(BlockedDomainResponse::from);
    }

    @Transactional
    public void remove(Long id) {
        BlockedDomain bd = blockedDomainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlockedDomain", "id", id));
        blockedDomainRepository.delete(bd);
        log.info("Blocked domain removed: {}", bd.getDomain());
    }
}
