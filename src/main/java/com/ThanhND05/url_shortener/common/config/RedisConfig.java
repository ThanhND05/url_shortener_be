package com.ThanhND05.url_shortener.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Redis & Cache configuration.
 * Caching is enabled regardless of the backend (simple / redis).
 * Set spring.cache.type=redis once Redis is running.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", matchIfMissing = true)
public class RedisConfig {
}
