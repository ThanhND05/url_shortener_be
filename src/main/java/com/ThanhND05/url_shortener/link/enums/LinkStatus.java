package com.ThanhND05.url_shortener.link.enums;

/**
 * Trạng thái của short link.
 *
 * - ACTIVE:      link hoạt động, redirect bình thường.
 * - DISABLED:    tạm tắt bởi chủ sở hữu, trả 410 Gone.
 * - EXPIRED:     đã quá expires_at, không redirect nữa.
 * - DELETED:     soft-delete, không hiển thị trong danh sách.
 * - QUARANTINED: bị gắn cờ nghi ngờ (phishing/malware), chờ review.
 */
public enum LinkStatus {
    ACTIVE,
    DISABLED,
    EXPIRED,
    DELETED,
    QUARANTINED
}
