package com.ThanhND05.url_shortener.analytics.api;

import com.ThanhND05.url_shortener.analytics.api.dto.LinkCounterApiDto;
import com.ThanhND05.url_shortener.analytics.entity.LinkCounter;
import com.ThanhND05.url_shortener.analytics.repository.LinkCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsPublicApiImpl implements AnalyticsPublicApi {
    private final LinkCounterRepository linkCounterRepository;

    @Override
    public Map<Long, LinkCounterApiDto> getCountersByLinkIds(List<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) return Map.of();
        return linkCounterRepository.findAllById(linkIds).stream()
                .collect(Collectors.toMap(
                        LinkCounter::getLinkId,
                        c -> new LinkCounterApiDto(c.getLinkId(), c.getTotalClicks(), c.getUniqueVisitorsEstimate(), c.getLastClickedAt())
                ));
    }

    @Override
    public LinkCounterApiDto getCounterByLinkId(Long linkId) {
        if (linkId == null) return null;
        return linkCounterRepository.findById(linkId)
                .map(c -> new LinkCounterApiDto(c.getLinkId(), c.getTotalClicks(), c.getUniqueVisitorsEstimate(), c.getLastClickedAt()))
                .orElse(null);
    }
}
