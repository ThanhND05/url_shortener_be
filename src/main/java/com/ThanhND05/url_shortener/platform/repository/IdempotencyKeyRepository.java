package com.ThanhND05.url_shortener.platform.repository;

import com.ThanhND05.url_shortener.platform.entity.IdempotencyKey;
import com.ThanhND05.url_shortener.platform.entity.IdempotencyKeyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {

    /** Xóa keys đã hết hạn — gọi bởi scheduled cleanup job. */
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpiredKeys(Instant now);
}
