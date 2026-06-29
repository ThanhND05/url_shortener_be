package com.ThanhND05.url_shortener.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA configuration — enables auditing so that @CreatedDate / @LastModifiedDate
 * and @CreatedBy / @LastModifiedBy annotations work on entities.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                try {
                    return Optional.of(UUID.fromString(jwt.getSubject()));
                } catch (IllegalArgumentException e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        };
    }
}
