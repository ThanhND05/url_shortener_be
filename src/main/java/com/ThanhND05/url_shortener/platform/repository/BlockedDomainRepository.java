package com.ThanhND05.url_shortener.platform.repository;

import com.ThanhND05.url_shortener.platform.entity.BlockedDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedDomainRepository extends JpaRepository<BlockedDomain, Long> {

    boolean existsByDomain(String domain);

    Page<BlockedDomain> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
