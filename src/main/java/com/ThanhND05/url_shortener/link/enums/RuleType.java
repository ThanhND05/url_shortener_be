package com.ThanhND05.url_shortener.link.enums;

/**
 * Loại routing rule — điều hướng người dùng tới URL khác nhau tùy điều kiện.
 *
 * - COUNTRY:  theo quốc gia (geo-IP). VD: VN → trang tiếng Việt.
 * - DEVICE:   theo loại thiết bị (mobile/desktop/tablet).
 * - LANGUAGE: theo ngôn ngữ trình duyệt (Accept-Language header).
 * - TIME:     theo khung giờ (VD: chỉ active 9h-17h).
 * - AB_TEST:  phân chia traffic theo tỷ lệ % (A/B testing).
 */
public enum RuleType {
    COUNTRY,
    DEVICE,
    LANGUAGE,
    TIME,
    AB_TEST
}
