package com.ThanhND05.url_shortener.analytics.service;

import com.ThanhND05.url_shortener.analytics.entity.ClickEvent;
import com.ThanhND05.url_shortener.analytics.repository.ClickEventRepository;
import com.ThanhND05.url_shortener.analytics.repository.LinkCounterRepository;
import com.ThanhND05.url_shortener.common.util.HashUtil;
import com.ThanhND05.url_shortener.link.event.LinkClickedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Service xử lý click ingestion — nhận LinkClickedEvent từ redirect flow.
 *
 * === FLOW INGESTION ===
 * 1. RedirectService publish LinkClickedEvent (async).
 * 2. @EventListener trong class này nhận event.
 * 3. Parse User-Agent → device_type, os, browser, is_bot.
 * 4. Tính visitor_hash = SHA-256(ip + userAgent) → xấp xỉ unique visitor.
 * 5. Trích xuất referer_domain từ referer URL.
 * 6. INSERT vào analytics.click_events.
 * 7. UPSERT analytics.link_counters (atomic increment).
 *
 * Lưu ý:
 * - @Async: xử lý trên thread riêng, KHÔNG block redirect response.
 * - Geo-location (country, region, city): để null (cần tích hợp GeoIP service, phase sau).
 * - User-Agent parsing: dùng regex đơn giản, có thể nâng cấp lên thư viện như ua-parser.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClickIngestionService {

    private final ClickEventRepository clickEventRepository;
    private final LinkCounterRepository linkCounterRepository;

    // Regex đơn giản phát hiện bot — check các pattern phổ biến
    private static final Pattern BOT_PATTERN = Pattern.compile(
            "(?i)(bot|crawl|spider|slurp|mediapartners|adsbot|bingpreview|facebookexternalhit)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Listener nhận LinkClickedEvent — xử lý async, không block redirect.
     */
    @Async
    @EventListener
    @Transactional
    public void handleClickEvent(LinkClickedEvent event) {
        try {
            Instant now = Instant.now();
            String userAgent = event.userAgent() != null ? event.userAgent() : "";
            String referer = event.referer();

            // Parse User-Agent
            boolean isBot = BOT_PATTERN.matcher(userAgent).find();
            String deviceType = parseDeviceType(userAgent);
            String os = parseOs(userAgent);
            String browser = parseBrowser(userAgent);

            // Tính visitor hash = SHA-256(ip_hash_hex + userAgent) → unique visitor tracking
            String visitorKey = (event.ipHash() != null ? java.util.HexFormat.of().formatHex(event.ipHash()) : "")
                    + userAgent;
            byte[] visitorHash = HashUtil.sha256Bytes(visitorKey);

            // Trích xuất domain từ referer URL
            String refererDomain = extractDomain(referer);

            // 1. INSERT click event
            ClickEvent clickEvent = ClickEvent.builder()
                    .occurredAt(now)
                    .linkId(event.linkId())
                    .linkPublicId(event.linkPublicId())
                    .domainId(event.domainId())
                    .shortCode(event.shortCode())
                    .ipHash(event.ipHash())
                    .visitorHash(visitorHash)
                    .userAgentHash(HashUtil.sha256Bytes(userAgent))
                    .referer(referer)
                    .refererDomain(refererDomain)
                    .deviceType(deviceType)
                    .os(os)
                    .browser(browser)
                    .isBot(isBot)
                    .build();

            clickEventRepository.save(clickEvent);

            // 2. UPSERT link counter (atomic increment)
            linkCounterRepository.incrementClickCount(event.linkId(), now);

            log.debug("Click ingested: link={} shortCode={} device={} bot={}",
                    event.linkId(), event.shortCode(), deviceType, isBot);

        } catch (Exception e) {
            // Không throw — analytics failure KHÔNG ảnh hưởng redirect
            log.error("Failed to ingest click event for link {}: {}",
                    event.linkId(), e.getMessage(), e);
        }
    }

    // ── User-Agent Parsing (Simple) ──────────────────────

    /**
     * Parse device type từ User-Agent.
     * Logic đơn giản: check keyword → mobile > tablet > desktop.
     */
    private String parseDeviceType(String ua) {
        if (ua.isEmpty()) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") && !lower.contains("tablet")) return "mobile";
        if (lower.contains("tablet") || lower.contains("ipad")) return "tablet";
        return "desktop";
    }

    /** Parse OS từ User-Agent. */
    private String parseOs(String ua) {
        if (ua.isEmpty()) return "unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "other";
    }

    /** Parse browser từ User-Agent. */
    private String parseBrowser(String ua) {
        if (ua.isEmpty()) return "unknown";
        // Thứ tự check quan trọng — Edge/Opera chứa "Chrome", Chrome chứa "Safari"
        if (ua.contains("Edg/") || ua.contains("Edge/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Chrome/") && !ua.contains("Chromium/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Safari";
        return "other";
    }

    /** Trích xuất domain từ URL (VD: "https://www.facebook.com/path" → "facebook.com"). */
    private String extractDomain(String url) {
        if (url == null || url.isBlank()) return "direct";
        try {
            String host = URI.create(url).getHost();
            if (host == null) return "direct";
            // Loại bỏ "www." prefix
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
