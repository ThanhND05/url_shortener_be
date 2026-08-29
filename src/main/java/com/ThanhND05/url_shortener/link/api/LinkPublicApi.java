package com.ThanhND05.url_shortener.link.api;

import com.ThanhND05.url_shortener.link.api.dto.LinkApiDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LinkPublicApi {
    long countLinksByStatusNotDeleted();
    long countLinksCreatedBetween(Instant from, Instant to);
    long countLinksByStatus(String status);
    Map<Long, LinkApiDto> getLinksByIds(List<Long> linkIds);
}
