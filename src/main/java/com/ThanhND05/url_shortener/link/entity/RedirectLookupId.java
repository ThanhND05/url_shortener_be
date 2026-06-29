package com.ThanhND05.url_shortener.link.entity;

import lombok.*;

import java.io.Serializable;

/**
 * Composite Primary Key cho bảng link.redirect_lookup.
 * Gồm (domainId, shortCode) — xác định duy nhất một short link trên một domain.
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RedirectLookupId implements Serializable {
    private Long domainId;
    private String shortCode;
}
