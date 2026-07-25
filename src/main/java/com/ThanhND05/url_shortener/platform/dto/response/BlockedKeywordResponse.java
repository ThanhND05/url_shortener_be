package com.ThanhND05.url_shortener.platform.dto.response;

import com.ThanhND05.url_shortener.platform.entity.BlockedKeyword;
import lombok.Builder;

import java.time.Instant;

@Builder
public record BlockedKeywordResponse(
        Long id,
        String keyword,
        String reason,
        String createdBy,
        Instant createdAt
) {
    public static BlockedKeywordResponse from(BlockedKeyword bk) {
        return BlockedKeywordResponse.builder()
                .id(bk.getId())
                .keyword(bk.getKeyword())
                .reason(bk.getReason())
                .createdBy(bk.getCreatedBy())
                .createdAt(bk.getCreatedAt())
                .build();
    }
}
