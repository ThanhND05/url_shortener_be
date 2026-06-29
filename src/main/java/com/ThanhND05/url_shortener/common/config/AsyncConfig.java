package com.ThanhND05.url_shortener.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables async processing (for analytics event handling)
 * and scheduled tasks (for aggregation jobs, cleanup).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
