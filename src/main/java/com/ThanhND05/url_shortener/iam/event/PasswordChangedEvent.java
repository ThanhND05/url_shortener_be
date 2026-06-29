package com.ThanhND05.url_shortener.iam.event;

import java.util.UUID;

/**
 * Event phát hành khi user đổi mật khẩu thành công.
 * Listeners:
 * - IAM module: revoke tất cả refresh tokens + blacklist access tokens.
 * - Platform module: ghi audit log "PASSWORD_CHANGED".
 */
public record PasswordChangedEvent(UUID userId) {}
