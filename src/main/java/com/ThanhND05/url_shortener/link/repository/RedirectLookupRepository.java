package com.ThanhND05.url_shortener.link.repository;

import com.ThanhND05.url_shortener.link.entity.RedirectLookup;
import com.ThanhND05.url_shortener.link.entity.RedirectLookupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RedirectLookupRepository extends JpaRepository<RedirectLookup, RedirectLookupId> {

    /** Tìm redirect info bằng domain + shortCode — hot path chính. */
    Optional<RedirectLookup> findByDomainIdAndShortCode(Long domainId, String shortCode);

    /** Tìm bằng shortCode (bỏ qua domain) — dùng khi chỉ có 1 domain mặc định. */
    Optional<RedirectLookup> findFirstByShortCodeAndStatus(String shortCode, String status);
}
