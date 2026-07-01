package com.ThanhND05.url_shortener.link.repository;

import com.ThanhND05.url_shortener.link.entity.Domain;
import com.ThanhND05.url_shortener.link.enums.DomainStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {

    Optional<Domain> findByPublicId(UUID publicId);

    Optional<Domain> findByDomain(String domain);

    /** Tìm domain mặc định của user — mỗi user chỉ có tối đa 1. */
    Optional<Domain> findByOwnerIdAndIsDefaultTrue(UUID ownerId);

    /** Tìm domain mặc định của hệ thống (owner = null) */
    Optional<Domain> findByOwnerIdIsNullAndIsDefaultTrue();

    Page<Domain> findByOwnerIdAndStatusNot(UUID ownerId, DomainStatus status, Pageable pageable);

    boolean existsByDomain(String domain);
}
