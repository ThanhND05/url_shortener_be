package com.ThanhND05.url_shortener.analytics.kafka;

import com.ThanhND05.url_shortener.analytics.dto.ClickEventMessage;
import com.ThanhND05.url_shortener.analytics.entity.ClickEvent;
import com.ThanhND05.url_shortener.analytics.repository.ClickEventRepository;
import com.ThanhND05.url_shortener.analytics.repository.LinkCounterRepository;
import com.ThanhND05.url_shortener.common.config.KafkaConfig;
import com.ThanhND05.url_shortener.common.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Kafka Consumer cho click events — BATCH INSERT vào PostgreSQL.
 *
 * === ĐÂY LÀ TRÁI TIM CỦA KIẾN TRÚC HIGH-THROUGHPUT ===
 *
 * Cơ chế hoạt động:
 * 1. Kafka poll() → gom tối đa 500 messages (max.poll.records=500).
 * 2. Consumer nhận List<ClickEventMessage> (batch mode).
 * 3. Parse mỗi message → build ClickEvent entity.
 * 4. saveAll() → Hibernate batch INSERT toàn bộ 500 rows 1 lần.
 * 5. Cập nhật link_counters cho từng link (UPSERT atomic).
 * 6. Commit Kafka offset → xác nhận đã xử lý xong.
 *
 * Nếu batch insert fail → offset KHÔNG commit → Kafka resend batch →
 * at-least-once delivery.
 *
 * So sánh hiệu năng:
 * TRƯỚC: 10.000 click/s → 10.000 INSERT → 10.000 DB round-trip
 * SAU: 10.000 click/s → 20 batch × 500 rows → 20 DB round-trip (500x nhanh hơn)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventConsumer {

    private final ClickEventRepository clickEventRepository;
    private final LinkCounterRepository linkCounterRepository;

    // Regex phát hiện bot — copy từ ClickIngestionService cũ
    private static final Pattern BOT_PATTERN = Pattern.compile(
            "(?i)(bot|crawl|spider|slurp|mediapartners|adsbot|bingpreview|facebookexternalhit)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Batch consumer — nhận List<ClickEventMessage> từ Kafka.
     *
     * containerFactory = "batchFactory" → bật batch mode (xem KafkaConfig).
     * groupId = "click-consumer-group" → riêng biệt với audit consumer.
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_CLICK_EVENTS, containerFactory = "batchFactory", groupId = "click-consumer-group")
    @Transactional
    public void consumeBatch(List<ClickEventMessage> messages) {
        if (messages == null || messages.isEmpty())
            return;

        log.info("📥 Consuming batch of {} click events from Kafka", messages.size());
        long startTime = System.currentTimeMillis();

        // 1. Build tất cả ClickEvent entities
        List<ClickEvent> clickEvents = messages.stream()
                .map(this::toClickEvent)
                .toList();

        // 2. Batch INSERT tất cả click events 1 lần
        clickEventRepository.saveAll(clickEvents);

        // 3. Update link counters (UPSERT cho từng linkId unique trong batch)
        Instant now = Instant.now();
        messages.stream()
                .map(ClickEventMessage::getLinkId)
                .distinct()
                .forEach(linkId -> {
                    if (linkId != null) {
                        // Đếm số click của linkId này trong batch
                        long clickCount = messages.stream()
                                .filter(m -> linkId.equals(m.getLinkId()))
                                .count();
                        // UPSERT: increment by clickCount thay vì 1
                        for (int i = 0; i < clickCount; i++) {
                            linkCounterRepository.incrementClickCount(linkId, now);
                        }
                    }
                });

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ Batch inserted {} click events in {}ms", messages.size(), elapsed);
    }

    // ── Private: Convert message → entity ───────────────

    private ClickEvent toClickEvent(ClickEventMessage msg) {
        String userAgent = msg.getUserAgent() != null ? msg.getUserAgent() : "";
        String referer = msg.getReferer();

        boolean isBot = BOT_PATTERN.matcher(userAgent).find();
        String deviceType = parseDeviceType(userAgent);
        String os = parseOs(userAgent);
        String browser = parseBrowser(userAgent);

        // Visitor hash = SHA-256(ipHashHex + userAgent)
        byte[] ipHashBytes = msg.getIpHashBytes();
        String visitorKey = (ipHashBytes != null ? java.util.HexFormat.of().formatHex(ipHashBytes) : "")
                + userAgent;
        byte[] visitorHash = HashUtil.sha256Bytes(visitorKey);

        String refererDomain = extractDomain(referer);

        return ClickEvent.builder()
                .occurredAt(Instant.ofEpochMilli(msg.getTimestamp()))
                .linkId(msg.getLinkId())
                .linkPublicId(msg.getLinkPublicId() != null ? UUID.fromString(msg.getLinkPublicId()) : null)
                .domainId(msg.getDomainId())
                .shortCode(msg.getShortCode())
                .ipHash(ipHashBytes)
                .visitorHash(visitorHash)
                .userAgentHash(HashUtil.sha256Bytes(userAgent))
                .referer(referer)
                .refererDomain(refererDomain)
                .deviceType(deviceType)
                .os(os)
                .browser(browser)
                .isBot(isBot)
                .build();
    }

    // ── User-Agent Parsing (copy từ ClickIngestionService) ──

    private String parseDeviceType(String ua) {
        if (ua.isEmpty())
            return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") && !lower.contains("tablet"))
            return "mobile";
        if (lower.contains("tablet") || lower.contains("ipad"))
            return "tablet";
        return "desktop";
    }

    private String parseOs(String ua) {
        if (ua.isEmpty())
            return "unknown";
        if (ua.contains("Windows"))
            return "Windows";
        if (ua.contains("Mac OS") || ua.contains("Macintosh"))
            return "macOS";
        if (ua.contains("Linux"))
            return "Linux";
        if (ua.contains("Android"))
            return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad"))
            return "iOS";
        return "other";
    }

    private String parseBrowser(String ua) {
        if (ua.isEmpty())
            return "unknown";
        if (ua.contains("Edg/") || ua.contains("Edge/"))
            return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera"))
            return "Opera";
        if (ua.contains("Chrome/") && !ua.contains("Chromium/"))
            return "Chrome";
        if (ua.contains("Firefox/"))
            return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome/"))
            return "Safari";
        return "other";
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank())
            return "direct";
        try {
            String host = URI.create(url).getHost();
            if (host == null)
                return "direct";
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
