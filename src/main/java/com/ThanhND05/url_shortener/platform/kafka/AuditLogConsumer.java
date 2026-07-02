package com.ThanhND05.url_shortener.platform.kafka;

import com.ThanhND05.url_shortener.common.config.KafkaConfig;
import com.ThanhND05.url_shortener.platform.dto.AuditLogMessage;
import com.ThanhND05.url_shortener.platform.entity.AuditLog;
import com.ThanhND05.url_shortener.platform.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka Consumer cho audit log events — BATCH INSERT vào PostgreSQL.
 *
 * TRƯỚC: AuditEventListener → AuditService.logAction() → INSERT 1-by-1.
 * SAU:   AuditEventListener → KafkaProducer → AuditLogConsumer batch INSERT.
 *
 * Tuy audit events volume thấp hơn click events, nhưng batch insert
 * vẫn có lợi:
 *   - Khi có hàng nghìn user đăng ký/đăng nhập cùng lúc (event campaign).
 *   - Giảm contention trên bảng platform.audit_logs.
 *   - Server crash → audit events vẫn nằm trong Kafka → không mất log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogConsumer {

    private final AuditLogRepository auditLogRepository;

    /**
     * Batch consumer — nhận List<AuditLogMessage> từ Kafka.
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_AUDIT_EVENTS,
            containerFactory = "batchFactory",
            groupId = "audit-consumer-group"
    )
    @Transactional
    public void consumeBatch(List<AuditLogMessage> messages) {
        if (messages == null || messages.isEmpty()) return;

        log.info("📥 Consuming batch of {} audit events from Kafka", messages.size());
        long startTime = System.currentTimeMillis();

        // Map messages → AuditLog entities
        List<AuditLog> auditLogs = messages.stream()
                .map(this::toAuditLog)
                .toList();

        // Batch INSERT tất cả
        auditLogRepository.saveAll(auditLogs);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ Batch inserted {} audit logs in {}ms", messages.size(), elapsed);
    }

    private AuditLog toAuditLog(AuditLogMessage msg) {
        return AuditLog.builder()
                .actorId(msg.getActorId() != null ? UUID.fromString(msg.getActorId()) : null)
                .action(msg.getAction())
                .resourceType(msg.getResourceType())
                .resourceId(msg.getResourceId())
                .metadata(msg.getMetadata() != null ? msg.getMetadata() : "{}")
                .createdAt(Instant.ofEpochMilli(msg.getTimestamp()))
                .build();
    }
}
