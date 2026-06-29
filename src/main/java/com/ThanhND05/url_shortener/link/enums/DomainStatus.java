package com.ThanhND05.url_shortener.link.enums;

/**
 * Trạng thái của custom domain.
 *
 * - PENDING: mới đăng ký, chưa xác minh quyền sở hữu.
 * - ACTIVE:  đã xác minh, có thể tạo short link trên domain này.
 * - BLOCKED: bị chặn bởi admin (vi phạm chính sách).
 * - DELETED: đã xóa mềm.
 */
public enum DomainStatus {
    PENDING,
    ACTIVE,
    BLOCKED,
    DELETED
}
