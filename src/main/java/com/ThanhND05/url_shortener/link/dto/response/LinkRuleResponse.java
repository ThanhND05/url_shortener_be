package com.ThanhND05.url_shortener.link.dto.response;

import com.ThanhND05.url_shortener.link.entity.LinkRule;
import lombok.Builder;

@Builder
public record LinkRuleResponse(
        Long id, String ruleType, String condition,
        String targetUrl, int priority, boolean isActive
) {
    public static LinkRuleResponse from(LinkRule r) {
        return LinkRuleResponse.builder()
                .id(r.getId()).ruleType(r.getRuleType().name())
                .condition(r.getCondition()).targetUrl(r.getTargetUrl())
                .priority(r.getPriority()).isActive(r.isActive())
                .build();
    }
}
