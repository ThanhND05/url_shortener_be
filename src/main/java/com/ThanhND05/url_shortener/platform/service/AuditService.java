package com.ThanhND05.url_shortener.platform.service;

import com.ThanhND05.url_shortener.platform.dto.response.AuditLogResponse;
import com.ThanhND05.url_shortener.platform.entity.AuditLog;
import com.ThanhND05.url_shortener.platform.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service quản lý audit logs — ghi nhận và truy vấn lịch sử hành động.
 *
 * Ghi log:
 * - Không gọi trực tiếp từ business service.
 * - AuditEventListener lắng nghe domain events → gọi logAction() → INSERT audit log.
 *
 * Truy vấn:
 * - Admin dùng AuditLogController để xem lịch sử.
 * - Filter theo actor (user), resource (link/domain), hoặc tất cả.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Ghi 1 audit log record.
     * Được gọi bởi AuditEventListener, KHÔNG gọi trực tiếp từ service layer.
     */
    @Transactional
    public void logAction(UUID actorId, String action,
                          String resourceType, String resourceId,
                          String metadata) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .metadata(metadata != null ? metadata : "{}")
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit: {} by {} on {}:{}", action, actorId, resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listByActor(UUID actorId, Pageable pageable) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listByResource(String resourceType, String resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                resourceType, resourceId, pageable).map(AuditLogResponse::from);
    }
}
