package com.ThanhND05.url_shortener.billing.enums;

/**
 * Gói dịch vụ SaaS.
 *
 * FREE — Mặc định khi đăng ký:
 *   - Tối đa 50 links/tháng
 *   - Analytics chỉ 7 ngày gần nhất
 *   - Không được dùng Link Rules, A/B Testing, Custom Domain
 *
 * PRO (50.000đ/tháng):
 *   - Unlimited links
 *   - Analytics vĩnh viễn
 *   - Mở khóa: Link Rules, A/B Testing, Custom Domain
 */
public enum SubscriptionPlan {
    FREE,
    PRO
}
