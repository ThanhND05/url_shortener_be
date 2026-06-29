package com.ThanhND05.url_shortener.iam.event;

import java.util.UUID;

/**
 * Event phát hành khi tài khoản bị khóa (LOCKED).
 * Listeners:
 * - IAM module: revoke tất cả sessions + blacklist tokens.
 * - Platform module: ghi audit log "ACCOUNT_LOCKED".
 */
public record AccountLockedEvent(UUID userId, String reason) {}
