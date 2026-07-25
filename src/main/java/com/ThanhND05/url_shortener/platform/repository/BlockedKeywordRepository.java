package com.ThanhND05.url_shortener.platform.repository;

import com.ThanhND05.url_shortener.platform.entity.BlockedKeyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedKeywordRepository extends JpaRepository<BlockedKeyword, Long> {

    boolean existsByKeyword(String keyword);

    Page<BlockedKeyword> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Tìm kiếm keyword theo pattern (cho trang admin search).
     */
    @Query("SELECT bk FROM BlockedKeyword bk WHERE LOWER(bk.keyword) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY bk.createdAt DESC")
    Page<BlockedKeyword> searchByKeyword(String search, Pageable pageable);

    /**
     * Lấy tất cả keyword (dùng cho validation check, cache trong memory).
     */
    @Query("SELECT bk.keyword FROM BlockedKeyword bk")
    List<String> findAllKeywords();
}
