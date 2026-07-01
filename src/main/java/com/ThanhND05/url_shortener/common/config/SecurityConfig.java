package com.ThanhND05.url_shortener.common.config;

import com.ThanhND05.url_shortener.common.security.CustomJwtAuthenticationConverter;
import com.ThanhND05.url_shortener.common.security.JwtBlacklistValidator;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

/**
 * Central security configuration.
 * - Stateless JWT authentication via Spring OAuth2 Resource Server
 * - BCrypt password encoding
 * - CORS policy
 * - Public / protected endpoint rules
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AppProperties appProperties;
    private final JwtBlacklistValidator jwtBlacklistValidator;

    public SecurityConfig(AppProperties appProperties, JwtBlacklistValidator jwtBlacklistValidator) {
        this.appProperties = appProperties;
        this.jwtBlacklistValidator = jwtBlacklistValidator;
    }

    // ── Security Filter Chain ────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: auth endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Public: VNPay callback endpoints (IPN + Return URL)
                .requestMatchers("/api/v1/billing/vnpay-ipn", "/api/v1/billing/vnpay-return").permitAll()
                // Public: redirect short URLs
                .requestMatchers(HttpMethod.GET, "/r/**").permitAll()
                // Public: actuator health
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(new CustomJwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    // ── JWT Encoder / Decoder ────────────────────────────

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey key = getSecretKey();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = getSecretKey();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Thêm blacklist validator vào chuỗi validation:
        // 1. JwtTimestampValidator — check exp/nbf (mặc định)
        // 2. JwtBlacklistValidator — check JTI có trong blacklist (Redis → DB)
        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                jwtBlacklistValidator
        );
        decoder.setJwtValidator(validators);

        return decoder;
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes();
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // ── Password Encoder ─────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── CORS ─────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        String origins = appProperties.getCors().getAllowedOrigins();
        config.setAllowedOrigins(Arrays.asList(origins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
