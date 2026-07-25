package com.ThanhND05.url_shortener.platform.dto.response;

import com.ThanhND05.url_shortener.platform.entity.SystemConfig;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SystemConfigResponse(
        String configKey,
        String value,
        String description,
        Instant updatedAt,
        String updatedBy
) {
    public static SystemConfigResponse from(SystemConfig config) {
        return SystemConfigResponse.builder()
                .configKey(config.getConfigKey())
                .value(config.getValue())
                .description(config.getDescription())
                .updatedAt(config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy())
                .build();
    }
}
