package com.ThanhND05.url_shortener.link.dto.request;

import jakarta.validation.constraints.NotBlank;

/** DTO đăng ký custom domain. */
public record CreateDomainRequest(
        @NotBlank(message = "Tên domain không được để trống")
        String domain
) {}
