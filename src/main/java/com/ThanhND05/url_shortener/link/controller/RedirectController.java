package com.ThanhND05.url_shortener.link.controller;

import com.ThanhND05.url_shortener.link.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Controller xử lý redirect — PUBLIC endpoint, không yêu cầu auth.
 *
 * Flow:
 *   1. Client truy cập GET /r/{shortCode}
 *   2. RedirectService tìm link, validate (status, expiry, max clicks, password).
 *   3. Nếu OK → HTTP 301/302/307/308 redirect tới original URL.
 *   4. Publish LinkClickedEvent async → Analytics module ghi nhận.
 *
 * Tại sao dùng /r/ prefix?
 * - Tránh conflict với /api/v1/... và /actuator/... paths.
 * - Có thể cấu hình nginx rewrite root domain → /r/ cho production.
 */
@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;

    /**
     * Redirect short URL → original URL.
     * Extracts client info (IP, User-Agent, Referer) cho analytics tracking.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        // processRedirect sẽ validate + publish click event
        String originalUrl = redirectService.processRedirect(
                shortCode, clientIp, userAgent, referer);

        short redirectType = redirectService.getRedirectType(shortCode);
        HttpStatus status = switch (redirectType) {
            case 301 -> HttpStatus.MOVED_PERMANENTLY;
            case 307 -> HttpStatus.TEMPORARY_REDIRECT;
            case 308 -> HttpStatus.PERMANENT_REDIRECT;
            default -> HttpStatus.FOUND; // 302
        };

        return ResponseEntity.status(status)
                .header(HttpHeaders.LOCATION, originalUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    /**
     * Lấy IP thật của client — hỗ trợ reverse proxy (X-Forwarded-For).
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
