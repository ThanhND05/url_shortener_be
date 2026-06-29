package com.ThanhND05.url_shortener.iam.enums;

/**
 * Vai trò hệ thống gán khi tạo user — chỉ dùng để bootstrap RBAC ban đầu,
 * KHÔNG dùng để kiểm tra quyền lúc runtime (runtime dùng bảng user_roles + permissions).
 *
 * - USER:        người dùng thường, được gán role "member" khi đăng ký.
 * - SUPER_ADMIN: quản trị viên cao nhất, được gán role "super_admin".
 */
public enum SystemRole {
    USER,
    SUPER_ADMIN
}
