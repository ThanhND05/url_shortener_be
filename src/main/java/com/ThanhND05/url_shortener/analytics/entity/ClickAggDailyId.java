package com.ThanhND05.url_shortener.analytics.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Composite PK cho bảng analytics.click_agg_daily.
 * PK = (linkId, day).
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClickAggDailyId implements Serializable {
    private Long linkId;
    private LocalDate day;
}
