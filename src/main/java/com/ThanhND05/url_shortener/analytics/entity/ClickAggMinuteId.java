package com.ThanhND05.url_shortener.analytics.entity;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Composite PK cho bảng analytics.click_agg_minute.
 * PK = (linkId, bucketMinute).
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClickAggMinuteId implements Serializable {
    private Long linkId;
    private Instant bucketMinute;
}
