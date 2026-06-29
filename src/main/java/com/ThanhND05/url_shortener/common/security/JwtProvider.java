package com.ThanhND05.url_shortener.common.security;

import com.ThanhND05.url_shortener.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/**
 * Responsible for creating (encoding) and inspecting JWTs.
 * <p>
 * Access tokens contain userId, email, and effective permissions.
 * Refresh tokens are opaque random UUIDs stored hashed in the DB — not JWTs.
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;

    /**
     * Generate an access token JWT.
     *
     * @param userId      user's UUID (becomes the "sub" claim)
     * @param email       user's email
     * @param permissions effective permission slugs, e.g. {"link:create", "analytics:read"}
     * @return signed JWT string
     */
    public String generateAccessToken(UUID userId, String email, Set<String> permissions) {
        Instant now = Instant.now();
        long expirationMs = appProperties.getJwt().getAccessTokenExpirationMs();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("url-shortener")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(expirationMs, ChronoUnit.MILLIS))
                .id(UUID.randomUUID().toString())  // jti — used for token blacklist
                .claim("email", email)
                .claim("permissions", permissions)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Extract the JTI (JWT ID) from a raw token string.
     * Used when blacklisting an access token on password change / logout.
     */
    public String extractJti(String token) {
        // JwtDecoder is used by the framework; here we just parse the claims
        // For extracting JTI from a raw token, the caller should use JwtDecoder
        // and then jwt.getId(). This method is a convenience.
        return null; // Implemented via JwtDecoder in AuthService
    }

    /**
     * Generate a random refresh token value (NOT a JWT).
     * The raw value is returned to the client; the hash is stored in DB.
     */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    public long getAccessTokenExpirationMs() {
        return appProperties.getJwt().getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return appProperties.getJwt().getRefreshTokenExpirationMs();
    }
}
