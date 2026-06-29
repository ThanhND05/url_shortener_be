package com.ThanhND05.url_shortener.iam.enums;

/**
 * Trạng thái tài khoản người dùng.
 *
 * - ACTIVE:  tài khoản hoạt động bình thường, có thể đăng nhập.
 * - LOCKED:  bị khóa bởi admin (vi phạm, bảo mật), không thể đăng nhập.
 * - DELETED: đã bị xóa mềm (soft-delete), dữ liệu vẫn giữ trong DB.
 */
public enum UserStatus {
    ACTIVE,
    LOCKED,
    DELETED
}
