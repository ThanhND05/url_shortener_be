package com.ThanhND05.url_shortener.iam.repository;

import com.ThanhND05.url_shortener.iam.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository truy vấn bảng iam.token_blacklist.
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    /** Kiểm tra JTI có trong blacklist không — gọi khi có sự kiện bảo mật. */
    boolean existsByJti(UUID jti);

    /** Xóa các entry đã hết hạn — cleanup job chạy định kỳ. */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :now")
    int deleteExpired(Instant now);
}
