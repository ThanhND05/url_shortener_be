package com.ThanhND05.url_shortener.platform.dto.response;

import com.ThanhND05.url_shortener.platform.entity.BlockedDomain;
import lombok.Builder;

import java.time.Instant;

@Builder
public record BlockedDomainResponse(
        Long id, String domain, String reason,
        String source, Instant createdAt
) {
    public static BlockedDomainResponse from(BlockedDomain bd) {
        return BlockedDomainResponse.builder()
                .id(bd.getId()).domain(bd.getDomain())
                .reason(bd.getReason()).source(bd.getSource())
                .createdAt(bd.getCreatedAt())
                .build();
    }
}
