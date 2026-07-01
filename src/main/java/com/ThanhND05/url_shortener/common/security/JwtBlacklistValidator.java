package com.ThanhND05.url_shortener.common.security;

import com.ThanhND05.url_shortener.iam.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Custom JWT Validator kiểm tra access token có bị blacklist hay không.
 *
 * Validator này được thêm vào chuỗi validation của JwtDecoder.
 * Khi Spring Security decode JWT trên mỗi request, nó sẽ:
 *   1. Verify chữ ký (signature) — mặc định.
 *   2. Check expiration — mặc định.
 *   3. Check JTI có trong blacklist (Redis → DB fallback) — VALIDATOR NÀY.
 *
 * Nếu token bị blacklist → trả về OAuth2Error → Spring tự trả 401 Unauthorized.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtBlacklistValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error BLACKLISTED_TOKEN_ERROR =
            new OAuth2Error("invalid_token", "Access token đã bị vô hiệu hóa (blacklisted).", null);

    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String jti = jwt.getId();

        // Nếu JWT không có JTI → bỏ qua check (backward compatible)
        if (jti == null || jti.isBlank()) {
            return OAuth2TokenValidatorResult.success();
        }

        // Kiểm tra JTI trong blacklist (Redis → DB fallback)
        if (tokenBlacklistService.isBlacklisted(jti)) {
            log.warn("Blocked blacklisted access token with JTI: {}", jti);
            return OAuth2TokenValidatorResult.failure(BLACKLISTED_TOKEN_ERROR);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
