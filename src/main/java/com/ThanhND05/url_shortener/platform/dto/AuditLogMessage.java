package com.ThanhND05.url_shortener.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka message DTO cho audit log event — serialize/deserialize qua JSON.
 *
 * Tại sao dùng riêng DTO cho Kafka?
 * - Cần default constructor (Jackson deserialization).
 * - Tách biệt với domain event → thay đổi format message mà không ảnh hưởng business logic.
 *
 * Flow:
 *   AuditEventListener nhận domain event → map sang AuditLogMessage → Kafka topic
 *   → AuditLogConsumer batch INSERT vào platform.audit_logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogMessage {

    private String actorId;       // UUID as string (JSON-safe, null nếu system action)
    private String action;        // "USER_CREATED", "LINK_CREATED", "PASSWORD_CHANGED", ...
    private String resourceType;  // "User", "Link", "Domain", ...
    private String resourceId;    // ID resource bị tác động
    private String metadata;      // JSON metadata bổ sung
    private long timestamp;       // Epoch millis — thời điểm event xảy ra
}
