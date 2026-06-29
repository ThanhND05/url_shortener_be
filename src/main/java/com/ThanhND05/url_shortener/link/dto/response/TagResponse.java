package com.ThanhND05.url_shortener.link.dto.response;

import com.ThanhND05.url_shortener.link.entity.Tag;
import lombok.Builder;

import java.time.Instant;

@Builder
public record TagResponse(Long id, String name, Instant createdAt) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt());
    }
}
