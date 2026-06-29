package com.ThanhND05.url_shortener.common.security;

import com.ThanhND05.url_shortener.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/**
 * Utility class to extract information from the current SecurityContext.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // utility class
    }

    /**
     * Get the authenticated user's UUID from the JWT "sub" claim.
     */
    public static UUID getCurrentUserId() {
        Jwt jwt = getCurrentJwt();
        return UUID.fromString(jwt.getSubject());
    }

    /**
     * Get the authenticated user's email from the JWT "email" claim.
     */
    public static String getCurrentUserEmail() {
        Jwt jwt = getCurrentJwt();
        return jwt.getClaimAsString("email");
    }

    /**
     * Get the JTI (JWT ID) of the current access token.
     */
    public static String getCurrentJti() {
        Jwt jwt = getCurrentJwt();
        return jwt.getId();
    }

    /**
     * Get the raw JWT object from the security context.
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        throw new UnauthorizedException("Không có thông tin xác thực. Vui lòng đăng nhập.");
    }

    /**
     * Check if the current user has a specific permission authority.
     */
    public static boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
