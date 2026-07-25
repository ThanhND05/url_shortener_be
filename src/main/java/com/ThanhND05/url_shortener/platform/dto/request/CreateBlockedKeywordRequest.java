package com.ThanhND05.url_shortener.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO thêm keyword vào danh sách cấm.
 *
 * @param keyword từ khóa cần cấm (VD: "admin", "login", "phishing").
 * @param reason  lý do cấm (tùy chọn).
 */
public record CreateBlockedKeywordRequest(
        @NotBlank(message = "Keyword không được để trống")
        @Size(min = 2, max = 100, message = "Keyword phải từ 2-100 ký tự")
        String keyword,
        String reason
) {}
