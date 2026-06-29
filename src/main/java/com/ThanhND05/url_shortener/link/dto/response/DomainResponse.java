package com.ThanhND05.url_shortener.link.dto.response;

import com.ThanhND05.url_shortener.link.entity.Domain;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DomainResponse(
        UUID publicId, String domain, boolean isDefault,
        String status, Instant verifiedAt, Instant createdAt
) {
    public static DomainResponse from(Domain d) {
        return DomainResponse.builder()
                .publicId(d.getPublicId()).domain(d.getDomain())
                .isDefault(d.isDefault()).status(d.getStatus().name())
                .verifiedAt(d.getVerifiedAt()).createdAt(d.getCreatedAt())
                .build();
    }
}
