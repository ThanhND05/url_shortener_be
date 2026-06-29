package com.ThanhND05.url_shortener.analytics.dto.response;

import com.ThanhND05.url_shortener.analytics.entity.LinkCounter;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * DTO tổng hợp thống kê cho 1 link.
 * Bao gồm counters real-time + timeseries data.
 *
 * @param totalClicks           tổng lượt click tất cả thời gian.
 * @param uniqueVisitorsEstimate ước lượng unique visitors.
 * @param lastClickedAt         thời điểm click gần nhất.
 * @param timeseries            danh sách data points (minute hoặc daily tùy query).
 */
@Builder
public record LinkStatsResponse(
        long totalClicks,
        long uniqueVisitorsEstimate,
        Instant lastClickedAt,
        List<ClickAggResponse> timeseries
) {
    /** Tạo từ LinkCounter entity + timeseries data. */
    public static LinkStatsResponse from(LinkCounter counter, List<ClickAggResponse> timeseries) {
        return LinkStatsResponse.builder()
                .totalClicks(counter.getTotalClicks())
                .uniqueVisitorsEstimate(counter.getUniqueVisitorsEstimate())
                .lastClickedAt(counter.getLastClickedAt())
                .timeseries(timeseries)
                .build();
    }
}
