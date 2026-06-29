package com.ThanhND05.url_shortener.platform.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite PK cho bảng platform.idempotency_keys.
 * PK = (ownerId, idempotencyKey).
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IdempotencyKeyId implements Serializable {
    private UUID ownerId;
    private String idempotencyKey;
}
