package com.ThanhND05.url_shortener.billing.dto.response;

import com.ThanhND05.url_shortener.billing.entity.Subscription;
import lombok.Builder;

import java.time.Instant;

/**
 * Response chứa thông tin gói dịch vụ hiện tại của user.
 */
@Builder
public record SubscriptionResponse(
        String plan,
        String status,
        boolean isPro,
        Instant startedAt,
        Instant expiresAt,
        int linksUsed,
        int linksLimit
) {
    private static final int FREE_LINKS_LIMIT = 50;
    private static final int PRO_LINKS_LIMIT = Integer.MAX_VALUE;

    public static SubscriptionResponse from(Subscription sub) {
        boolean pro = sub.isPro();
        return SubscriptionResponse.builder()
                .plan(sub.getPlan().name())
                .status(sub.getStatus().name())
                .isPro(pro)
                .startedAt(sub.getStartedAt())
                .expiresAt(sub.getExpiresAt())
                .linksUsed(sub.getLinksUsed())
                .linksLimit(pro ? PRO_LINKS_LIMIT : FREE_LINKS_LIMIT)
                .build();
    }
}
