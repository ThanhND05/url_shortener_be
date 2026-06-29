package com.ThanhND05.url_shortener.iam.enums;

/**
 * Trạng thái của API Key.
 *
 * - ACTIVE:  key đang hoạt động, có thể dùng để gọi API.
 * - REVOKED: đã bị thu hồi bởi user hoặc admin.
 * - EXPIRED: đã hết hạn (quá expires_at).
 */
public enum ApiKeyStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}
