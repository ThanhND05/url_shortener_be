package com.ThanhND05.url_shortener.link.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO admin đổi trạng thái link.
 *
 * @param status trạng thái mới: ACTIVE, DISABLED, QUARANTINED, EXPIRED.
 * @param reason lý do thay đổi (tùy chọn — ghi log audit).
 */
public record AdminUpdateLinkStatusRequest(
        @NotBlank(message = "Status không được để trống")
        String status,
        String reason
) {}
