package com.ThanhND05.url_shortener.platform.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO thêm domain vào blacklist.
 *
 * @param domain domain cần chặn (VD: "evil-phishing.com").
 * @param reason lý do chặn (tùy chọn).
 * @param source nguồn thông tin: "manual", "google_safe_browsing", ... (tùy chọn).
 */
public record CreateBlockedDomainRequest(
        @NotBlank(message = "Domain không được để trống")
        String domain,
        String reason,
        String source
) {}
