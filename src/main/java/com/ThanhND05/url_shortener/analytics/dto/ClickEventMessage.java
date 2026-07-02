package com.ThanhND05.url_shortener.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Kafka message DTO cho click event — serialize/deserialize qua JSON.
 *
 * Tại sao KHÔNG dùng trực tiếp LinkClickedEvent (record)?
 * - Kafka cần default constructor để deserialize JSON → record không có.
 * - byte[] (ipHash) cần serialize đặc biệt → dùng Base64 string thay thế.
 * - DTO tách biệt giúp thay đổi Kafka message format mà không ảnh hưởng domain event.
 *
 * Flow:
 *   LinkClickedEvent → map sang ClickEventMessage → serialize JSON → Kafka topic
 *   → Consumer deserialize → parse + INSERT DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessage {

    private Long linkId;
    private String linkPublicId;   // UUID as string (JSON-safe)
    private Long domainId;
    private String shortCode;
    private String ipHashBase64;   // byte[] → Base64 string
    private String userAgent;
    private String referer;
    private long timestamp;        // Instant.toEpochMilli() — thời điểm click

    /**
     * Factory method: convert từ domain event sang Kafka message.
     */
    public static ClickEventMessage from(
            Long linkId, UUID linkPublicId, Long domainId, String shortCode,
            byte[] ipHash, String userAgent, String referer) {
        return ClickEventMessage.builder()
                .linkId(linkId)
                .linkPublicId(linkPublicId != null ? linkPublicId.toString() : null)
                .domainId(domainId)
                .shortCode(shortCode)
                .ipHashBase64(ipHash != null ? java.util.Base64.getEncoder().encodeToString(ipHash) : null)
                .userAgent(userAgent)
                .referer(referer)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** Helper: convert Base64 string back to byte[]. */
    public byte[] getIpHashBytes() {
        return ipHashBase64 != null ? java.util.Base64.getDecoder().decode(ipHashBase64) : null;
    }
}
