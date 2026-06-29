package com.ThanhND05.url_shortener.link.event;

import java.util.UUID;

/**
 * Event phát hành khi short link được tạo mới.
 * Listener: Platform module → ghi audit log.
 */
public record LinkCreatedEvent(Long linkId, UUID publicId, UUID ownerId, String shortCode) {}
