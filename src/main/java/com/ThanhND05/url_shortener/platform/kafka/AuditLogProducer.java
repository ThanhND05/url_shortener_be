package com.ThanhND05.url_shortener.platform.kafka;

import com.ThanhND05.url_shortener.common.config.KafkaConfig;
import com.ThanhND05.url_shortener.platform.dto.AuditLogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka Producer cho audit log events — bắn message vào topic "audit-events".
 *
 * TRƯỚC: AuditEventListener → auditService.logAction() → INSERT DB 1-by-1.
 * SAU:   AuditEventListener → auditLogProducer.send() → Kafka → batch INSERT.
 *
 * Key = actorId → cùng user vào cùng partition → giữ thứ tự audit log theo user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Gửi audit log message tới Kafka topic.
     *
     * @param message audit event data đã serialize-ready
     */
    public void send(AuditLogMessage message) {
        String key = message.getActorId() != null ? message.getActorId() : "system";
        kafkaTemplate.send(KafkaConfig.TOPIC_AUDIT_EVENTS, key, message);
        log.debug("Sent audit event to Kafka: action={}, actor={}",
                message.getAction(), message.getActorId());
    }
}
