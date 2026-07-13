package com.ThanhND05.url_shortener.link.repository;

import com.ThanhND05.url_shortener.link.entity.Link;
import com.ThanhND05.url_shortener.link.enums.LinkStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LinkSpecifications {

    private LinkSpecifications() {
    }

    public static Specification<Link> adminSearch(String search, LinkStatus status,
            UUID ownerId, Instant fromDate, Instant toDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (ownerId != null) {
                predicates.add(cb.equal(root.get("ownerId"), ownerId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("originalUrl")), pattern),
                        cb.like(cb.lower(root.get("shortCode")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
