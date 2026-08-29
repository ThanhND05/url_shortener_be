package com.ThanhND05.url_shortener.link.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record LinkApiDto(
        Long id,
        UUID publicId,
        String shortCode,
        String originalUrl,
        String title,
        UUID ownerId,
        String status,
        short redirectType,
        Set<String> tags,
        Instant createdAt
) {}
