package com.ThanhND05.url_shortener.link.dto.request;

import jakarta.validation.constraints.NotBlank;

/** DTO tạo tag mới. */
public record CreateTagRequest(
        @NotBlank(message = "Tên tag không được để trống")
        String name
) {}
