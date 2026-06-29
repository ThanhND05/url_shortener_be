package com.ThanhND05.url_shortener.analytics.entity;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Composite Primary Key cho bảng analytics.click_events.
 * PK = (occurred_at, event_id) — bắt buộc cho partitioned table (partition key phải nằm trong PK).
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClickEventId implements Serializable {
    private Instant occurredAt;
    private UUID eventId;
}
