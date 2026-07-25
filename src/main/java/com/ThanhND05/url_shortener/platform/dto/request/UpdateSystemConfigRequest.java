package com.ThanhND05.url_shortener.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO cập nhật giá trị system config.
 *
 * @param value giá trị mới cho config key.
 */
public record UpdateSystemConfigRequest(
        @NotNull(message = "Giá trị không được null")
        @NotBlank(message = "Giá trị không được để trống")
        String value
) {}
