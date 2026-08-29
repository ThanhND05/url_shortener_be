package com.ThanhND05.url_shortener.analytics.api;

import com.ThanhND05.url_shortener.analytics.api.dto.LinkCounterApiDto;

import java.util.List;
import java.util.Map;

public interface AnalyticsPublicApi {
    Map<Long, LinkCounterApiDto> getCountersByLinkIds(List<Long> linkIds);
    LinkCounterApiDto getCounterByLinkId(Long linkId);
}
