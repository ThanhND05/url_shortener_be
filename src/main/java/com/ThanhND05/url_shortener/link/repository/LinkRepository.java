package com.ThanhND05.url_shortener.link.repository;

import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.enums.LinkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long>, JpaSpecificationExecutor<Link> {

    Optional<Link> findByPublicId(UUID publicId);

    /** Kiểm tra short_code đã tồn tại trên domain. */
    boolean existsByDomainIdAndShortCode(Long domainId, String shortCode);

    /** Lấy links của user (trừ DELETED) sắp theo created_at DESC. */
    Page<Link> findByOwnerIdAndStatusNot(UUID ownerId, LinkStatus status, Pageable pageable);

    /** Tìm link trùng URL (theo hash) cho cùng owner — tránh tạo trùng. */
    Optional<Link> findByOwnerIdAndOriginalUrlHashAndStatusNot(
            UUID ownerId, String urlHash, LinkStatus status);

    /**
     * Lấy giá trị tiếp theo từ sequence link.short_code_seq — dùng cho Base62
     * encoding.
     */
    @Query(value = "SELECT nextval('link.short_code_seq')", nativeQuery = true)
    Long getNextShortCodeSequence();

    // ── ADMIN QUERIES ────────────────────────────────────

    /**
     * Admin: Tìm kiếm links toàn hệ thống (search + filter linh hoạt).
     *
     * - search: ILIKE trên original_url HOẶC short_code (null = bỏ qua).
     * - status: filter theo trạng thái (null = tất cả).
     * - ownerId: filter theo user tạo (null = tất cả).
     * - fromDate, toDate: filter theo khoảng thời gian tạo (null = bỏ qua).
     */
    // @Query("""
    // SELECT l FROM Link l
    // WHERE (:status IS NULL OR l.status = :status)
    // AND (:ownerId IS NULL OR l.ownerId = :ownerId)
    // AND (:fromDate IS NULL OR l.createdAt >= :fromDate)
    // AND (:toDate IS NULL OR l.createdAt <= :toDate)
    // AND (:search IS NULL OR :search = ''
    // OR LOWER(l.originalUrl) LIKE LOWER(CONCAT('%', :search, '%'))
    // OR LOWER(l.shortCode) LIKE LOWER(CONCAT('%', :search, '%')))
    // """)
    // Page<Link> adminSearch(String search, LinkStatus status, UUID ownerId,
    // Instant fromDate, Instant toDate, Pageable pageable);

    /** Admin: đếm tổng links (trừ DELETED). */
    long countByStatusNot(LinkStatus status);

    /** Admin: đếm links theo status. */
    long countByStatus(LinkStatus status);

    /** Admin: đếm links tạo trong khoảng thời gian. */
    long countByCreatedAtBetween(Instant from, Instant to);

    /** Admin: top links theo click count. */
    List<Link> findTopByStatusNotOrderByClickCountDesc(LinkStatus status, Pageable pageable);
}
