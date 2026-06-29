package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.link.dto.request.CreateDomainRequest;
import com.ThanhND05.url_shortener.link.dto.response.DomainResponse;
import com.ThanhND05.url_shortener.link.entity.Domain;
import com.ThanhND05.url_shortener.link.enums.DomainStatus;
import com.ThanhND05.url_shortener.link.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service quản lý custom domains.
 *
 * Flow đăng ký domain:
 * 1. User gửi domain name → server kiểm tra chưa tồn tại.
 * 2. Tạo bản ghi status = PENDING + sinh verification_token.
 * 3. User tạo DNS TXT record chứa token → gọi API verify.
 * 4. (Trong MVP: verify tự động set ACTIVE, skip DNS check thực tế).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainService {

    private final DomainRepository domainRepository;

    @Transactional
    public DomainResponse createDomain(UUID ownerId, CreateDomainRequest request) {
        if (domainRepository.existsByDomain(request.domain())) {
            throw new DuplicateResourceException("Domain", "domain", request.domain());
        }

        Domain domain = Domain.builder()
                .ownerId(ownerId)
                .domain(request.domain().toLowerCase().trim())
                .verificationToken("verify_" + UUID.randomUUID().toString().substring(0, 12))
                .build();
        domain = domainRepository.save(domain);
        log.info("Domain registered: {} by user {}", request.domain(), ownerId);
        return DomainResponse.from(domain);
    }

    @Transactional(readOnly = true)
    public Page<DomainResponse> listDomains(UUID ownerId, Pageable pageable) {
        return domainRepository.findByOwnerIdAndStatusNot(ownerId, DomainStatus.DELETED, pageable)
                .map(DomainResponse::from);
    }

    /** Xác minh domain — trong MVP, tự động set ACTIVE. */
    @Transactional
    public DomainResponse verifyDomain(UUID publicId, UUID ownerId) {
        Domain domain = findOwnedDomain(publicId, ownerId);
        if (domain.getStatus() != DomainStatus.PENDING) {
            throw new BusinessException("Domain đã được xác minh hoặc bị chặn.");
        }
        domain.setStatus(DomainStatus.ACTIVE);
        domain.setVerifiedAt(Instant.now());
        domain = domainRepository.save(domain);
        log.info("Domain verified: {}", domain.getDomain());
        return DomainResponse.from(domain);
    }

    /** Đặt làm domain mặc định (unique partial index đảm bảo chỉ 1). */
    @Transactional
    public DomainResponse setDefault(UUID publicId, UUID ownerId) {
        // Bỏ default cũ (nếu có)
        domainRepository.findByOwnerIdAndIsDefaultTrue(ownerId)
                .ifPresent(old -> { old.setDefault(false); domainRepository.save(old); });
        // Set default mới
        Domain domain = findOwnedDomain(publicId, ownerId);
        domain.setDefault(true);
        domain = domainRepository.save(domain);
        return DomainResponse.from(domain);
    }

    @Transactional
    public void deleteDomain(UUID publicId, UUID ownerId) {
        Domain domain = findOwnedDomain(publicId, ownerId);
        domain.setStatus(DomainStatus.DELETED);
        domainRepository.save(domain);
    }

    /** Tìm domain mặc định hoặc domain đầu tiên ACTIVE của user. */
    @Transactional(readOnly = true)
    public Domain resolveDefaultDomain(UUID ownerId) {
        return domainRepository.findByOwnerIdAndIsDefaultTrue(ownerId)
                .orElseThrow(() -> new BusinessException("Bạn chưa có domain. Vui lòng tạo domain trước."));
    }

    private Domain findOwnedDomain(UUID publicId, UUID ownerId) {
        Domain domain = domainRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain", "publicId", publicId));
        if (!ownerId.equals(domain.getOwnerId())) {
            throw new ResourceNotFoundException("Domain", "publicId", publicId);
        }
        return domain;
    }
}
