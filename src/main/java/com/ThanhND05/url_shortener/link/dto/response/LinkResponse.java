package com.ThanhND05.url_shortener.link.dto.response;

import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.entity.Tag;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO phản hồi thông tin đầy đủ của short link.
 * Bao gồm short URL hoàn chỉnh (domain + shortCode) và danh sách tag names.
 */
@Builder
public record LinkResponse(
        UUID publicId, String shortCode, String shortCodeType,
        String originalUrl, String title, String description,
        String status, short redirectType,
        Instant startsAt, Instant expiresAt, Long maxClicks,
        long clickCount, Instant lastClickedAt,
        boolean passwordProtected, Set<String> tags,
        Instant createdAt, Instant updatedAt
) {
    public static LinkResponse from(Link link) {
        Set<String> tagNames = link.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        return LinkResponse.builder()
                .publicId(link.getPublicId())
                .shortCode(link.getShortCode())
                .shortCodeType(link.getShortCodeType().name())
                .originalUrl(link.getOriginalUrl())
                .title(link.getTitle())
                .description(link.getDescription())
                .status(link.getStatus().name())
                .redirectType(link.getRedirectType())
                .startsAt(link.getStartsAt())
                .expiresAt(link.getExpiresAt())
                .maxClicks(link.getMaxClicks())
                .clickCount(link.getClickCount())
                .lastClickedAt(link.getLastClickedAt())
                .passwordProtected(link.isPasswordProtected())
                .tags(tagNames)
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }
}
