package com.ThanhND05.url_shortener.link.repository;

import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.enums.LinkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByPublicId(UUID publicId);

    /** Kiểm tra short_code đã tồn tại trên domain. */
    boolean existsByDomainIdAndShortCode(Long domainId, String shortCode);

    /** Lấy links của user (trừ DELETED) sắp theo created_at DESC. */
    Page<Link> findByOwnerIdAndStatusNot(UUID ownerId, LinkStatus status, Pageable pageable);

    /** Tìm link trùng URL (theo hash) cho cùng owner — tránh tạo trùng. */
    Optional<Link> findByOwnerIdAndOriginalUrlHashAndStatusNot(
            UUID ownerId, String urlHash, LinkStatus status);

    /** Lấy giá trị tiếp theo từ sequence link.short_code_seq — dùng cho Base62 encoding. */
    @Query(value = "SELECT nextval('link.short_code_seq')", nativeQuery = true)
    Long getNextShortCodeSequence();
}
