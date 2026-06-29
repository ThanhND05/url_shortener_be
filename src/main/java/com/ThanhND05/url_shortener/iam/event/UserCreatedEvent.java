package com.ThanhND05.url_shortener.iam.event;

import java.util.UUID;

/**
 * Event phát hành khi user mới được tạo thành công.
 * Listener: Platform module → ghi audit log "USER_REGISTERED".
 */
public record UserCreatedEvent(UUID userId, String email) {}
