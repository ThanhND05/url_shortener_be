package com.ThanhND05.url_shortener.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds custom application properties under the "app" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs = 900_000;       // 15 min
        private long refreshTokenExpirationMs = 604_800_000;   // 7 days
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }
}
