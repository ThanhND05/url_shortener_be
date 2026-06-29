package com.ThanhND05.url_shortener.iam.repository;

import com.ThanhND05.url_shortener.iam.entity.ApiKey;
import com.ThanhND05.url_shortener.iam.enums.ApiKeyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /** Tìm API key bằng hash — dùng khi authenticate từ header X-API-Key. */
    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

    /** Lấy tất cả key của user (phân trang). */
    Page<ApiKey> findByOwnerId(UUID ownerId, Pageable pageable);
}
