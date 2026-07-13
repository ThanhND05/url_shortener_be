package com.ThanhND05.url_shortener.iam.repository;

import com.ThanhND05.url_shortener.iam.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByResourceAndAction(String resource, String action);

    /** Kiểm tra cặp (resource, action) đã tồn tại chưa — dùng khi tạo/sửa permission. */
    boolean existsByResourceAndAction(String resource, String action);
}
