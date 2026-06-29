package com.ThanhND05.url_shortener.iam.repository;

import com.ThanhND05.url_shortener.iam.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /** Lấy tất cả role assignments của user — dùng để build danh sách permissions. */
    List<UserRole> findByUserId(UUID userId);

    /** Kiểm tra user đã có role cụ thể chưa (trong scope global). */
    boolean existsByUserIdAndRoleIdAndScopeId(UUID userId, Long roleId, UUID scopeId);
}
