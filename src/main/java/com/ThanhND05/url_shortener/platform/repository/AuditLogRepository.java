package com.ThanhND05.url_shortener.platform.repository;

import com.ThanhND05.url_shortener.platform.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Lấy audit logs theo actor (phân trang, mới nhất trước). */
    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    /** Lấy audit logs theo resource (VD: tất cả action trên 1 link). */
    Page<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, String resourceId, Pageable pageable);

    /** Lấy tất cả audit logs (admin, phân trang). */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
