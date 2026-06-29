package com.ThanhND05.url_shortener.link.event;

import java.util.UUID;

/**
 * Event phát hành khi short link được click (redirect thành công).
 * Listener: Analytics module → ghi click_event + cập nhật counters.
 *
 * @param linkId       internal ID — dùng cho FK trong analytics.
 * @param linkPublicId public UUID — cho API response.
 * @param domainId     domain ID — cho phân tích theo domain.
 * @param shortCode    short code — cho phân tích nhanh.
 * @param ipHash       hash IP client — cho unique visitor tracking.
 * @param userAgent    raw user-agent — analytics module sẽ parse device/browser/OS.
 * @param referer      HTTP Referer header — nguồn traffic.
 */
public record LinkClickedEvent(
        Long linkId, UUID linkPublicId,
        Long domainId, String shortCode,
        byte[] ipHash, String userAgent, String referer
) {}
