package com.ThanhND05.url_shortener.iam.repository;

import com.ThanhND05.url_shortener.iam.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy vấn bảng iam.refresh_tokens.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Tìm token bằng hash — dùng khi client gửi refresh token lên. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Lấy tất cả token cùng family (chuỗi rotation) — dùng khi phát hiện reuse. */
    List<RefreshToken> findByFamilyId(UUID familyId);

    /** Lấy tất cả token của user — dùng cho "logout tất cả". */
    List<RefreshToken> findByUserId(UUID userId);

    /** Lấy token theo session — dùng cho "logout device cụ thể". */
    List<RefreshToken> findBySessionId(UUID sessionId);

    /**
     * Revoke tất cả token của một user — dùng khi đổi mật khẩu hoặc bị khóa.
     * Chỉ revoke các token chưa bị revoke (revokedAt IS NULL).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now, rt.revokeReason = :reason " +
           "WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    int revokeAllByUserId(UUID userId, Instant now, String reason);

    /**
     * Revoke tất cả token cùng family — anti-theft: phát hiện token cũ bị reuse.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now, rt.revokeReason = 'FAMILY_REUSE' " +
           "WHERE rt.familyId = :familyId AND rt.revokedAt IS NULL")
    int revokeAllByFamilyId(UUID familyId, Instant now);
}
