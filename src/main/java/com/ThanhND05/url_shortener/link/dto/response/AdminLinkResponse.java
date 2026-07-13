package com.ThanhND05.url_shortener.link.dto.response;

import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.entity.Tag;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO phản hồi thông tin link dành cho admin — bao gồm ownerEmail.
 *
 * So với LinkResponse thông thường:
 * - Thêm ownerId, ownerEmail để admin biết ai tạo link.
 * - Dùng riêng cho các endpoint /api/v1/admin/links.
 */
@Builder
public record AdminLinkResponse(
        UUID publicId,
        UUID ownerId,
        String ownerEmail,
        String shortCode,
        String shortCodeType,
        String originalUrl,
        String title,
        String description,
        String status,
        short redirectType,
        Instant startsAt,
        Instant expiresAt,
        Long maxClicks,
        long clickCount,
        Instant lastClickedAt,
        boolean passwordProtected,
        Set<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Convert từ Link entity + ownerEmail sang DTO.
     *
     * @param link       Link entity.
     * @param ownerEmail email của owner (có thể null nếu anonymous link).
     */
    public static AdminLinkResponse from(Link link, String ownerEmail) {
        return from(link, ownerEmail, link.getClickCount(), link.getLastClickedAt());
    }

    /**
     * Convert từ Link entity + ownerEmail + real clicks/last clicked sang DTO.
     */
    public static AdminLinkResponse from(Link link, String ownerEmail, long realClicks, java.time.Instant realLastClickedAt) {
        Set<String> tagNames = link.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        return AdminLinkResponse.builder()
                .publicId(link.getPublicId())
                .ownerId(link.getOwnerId())
                .ownerEmail(ownerEmail)
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
                .clickCount(realClicks)
                .lastClickedAt(realLastClickedAt)
                .passwordProtected(link.isPasswordProtected())
                .tags(tagNames)
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }
}
