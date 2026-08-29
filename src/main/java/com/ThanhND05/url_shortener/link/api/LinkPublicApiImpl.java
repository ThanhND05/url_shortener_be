package com.ThanhND05.url_shortener.link.api;

import com.ThanhND05.url_shortener.link.api.dto.LinkApiDto;
import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.entity.Tag;
import com.ThanhND05.url_shortener.link.enums.LinkStatus;
import com.ThanhND05.url_shortener.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkPublicApiImpl implements LinkPublicApi {
    private final LinkRepository linkRepository;

    @Override
    public long countLinksByStatusNotDeleted() {
        return linkRepository.countByStatusNot(LinkStatus.DELETED);
    }

    @Override
    public long countLinksCreatedBetween(Instant from, Instant to) {
        return linkRepository.countByCreatedAtBetween(from, to);
    }

    @Override
    public long countLinksByStatus(String status) {
        return linkRepository.countByStatus(LinkStatus.valueOf(status));
    }

    @Override
    public Map<Long, LinkApiDto> getLinksByIds(List<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) return Map.of();
        return linkRepository.findAllById(linkIds).stream()
                .collect(Collectors.toMap(
                        Link::getId,
                        this::mapToDto
                ));
    }

    private LinkApiDto mapToDto(Link link) {
        Set<String> tags = link.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        return new LinkApiDto(
                link.getId(),
                link.getPublicId(),
                link.getShortCode(),
                link.getOriginalUrl(),
                link.getTitle(),
                link.getOwnerId(),
                link.getStatus().name(),
                link.getRedirectType(),
                tags,
                link.getCreatedAt()
        );
    }
}
