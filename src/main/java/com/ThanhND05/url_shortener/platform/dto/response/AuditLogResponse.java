package com.ThanhND05.url_shortener.platform.dto.response;

import com.ThanhND05.url_shortener.platform.entity.AuditLog;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AuditLogResponse(
        Long id, UUID actorId, String action,
        String resourceType, String resourceId,
        String metadata, Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId()).actorId(log.getActorId())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
