package com.ThanhND05.url_shortener.iam.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.common.security.JwtProvider;
import com.ThanhND05.url_shortener.common.util.HashUtil;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.entity.*;
import com.ThanhND05.url_shortener.iam.enums.ScopeType;
import com.ThanhND05.url_shortener.iam.enums.SystemRole;
import com.ThanhND05.url_shortener.iam.enums.UserStatus;
import com.ThanhND05.url_shortener.iam.event.UserCreatedEvent;
import com.ThanhND05.url_shortener.iam.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý toàn bộ luồng xác thực: đăng ký, đăng nhập, refresh token, logout.
 *
 * === FLOW ĐĂNG KÝ ===
 * 1. Kiểm tra email chưa tồn tại.
 * 2. Hash mật khẩu bằng BCrypt → lưu user.
 * 3. Bootstrap RBAC: gán role "member" (hoặc "super_admin" nếu system_role = SUPER_ADMIN).
 * 4. Publish UserCreatedEvent → Platform module ghi audit log.
 * 5. Tạo token pair (access + refresh) → trả client.
 *
 * === FLOW ĐĂNG NHẬP ===
 * 1. Tìm user bằng email, check status = ACTIVE.
 * 2. Verify mật khẩu bằng BCrypt.
 * 3. Tải permissions (từ user_roles → role_permissions → permissions).
 * 4. Tạo access token (JWT chứa userId, email, permissions).
 * 5. Tạo refresh token (random UUID, hash SHA-256 lưu DB).
 * 6. Trả cặp token cho client.
 *
 * === FLOW REFRESH TOKEN ===
 * 1. Hash token client gửi → tìm trong DB.
 * 2. Check: chưa revoke, chưa hết hạn.
 * 3. Nếu token ĐÃ bị revoke → phát hiện reuse → revoke TOÀN BỘ family.
 * 4. Nếu hợp lệ → revoke token cũ, tạo token mới (cùng family_id + session_id).
 *
 * === FLOW LOGOUT (ĐÃ TỐI ƯU) ===
 * - Logout đơn: revoke refresh token + blacklist access token hiện tại (Redis + DB).
 * - Logout tất cả: revoke tất cả refresh tokens + blacklist access token hiện tại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtDecoder jwtDecoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final ApplicationEventPublisher eventPublisher;

    // ── ĐĂNG KÝ ──────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra email trùng
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        // 2. Tạo user mới + hash mật khẩu bằng BCrypt
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .systemRole(SystemRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        // 3. Bootstrap RBAC: gán role "member" mặc định
        bootstrapUserRole(user);

        // 4. Publish event để các module khác xử lý (audit log, ...)
        eventPublisher.publishEvent(new UserCreatedEvent(user.getId(), user.getEmail()));

        log.info("User registered: {}", user.getEmail());

        // 5. Tạo token pair và trả về
        return buildAuthResponse(user);
    }

    // ── ĐĂNG NHẬP ─────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Tìm user, check tồn tại
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không đúng."));

        // 2. Check tài khoản chưa bị khóa/xóa
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }

        // 3. Verify mật khẩu bằng BCrypt
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không đúng.");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ── REFRESH TOKEN ─────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        // 1. Hash token → tìm trong DB
        String tokenHash = HashUtil.sha256Hex(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ."));

        // 2. Nếu token đã bị revoke → nghi ngờ token theft → revoke cả family
        if (storedToken.getRevokedAt() != null) {
            log.warn("Reuse detected for family {}. Revoking entire family.", storedToken.getFamilyId());
            refreshTokenRepository.revokeAllByFamilyId(storedToken.getFamilyId(), Instant.now());
            throw new UnauthorizedException("Phát hiện sử dụng token bất thường. Vui lòng đăng nhập lại.");
        }

        // 3. Kiểm tra hết hạn
        if (!storedToken.isValid()) {
            throw new UnauthorizedException("Refresh token đã hết hạn.");
        }

        // 4. Revoke token cũ
        storedToken.revoke("ROTATED");
        refreshTokenRepository.save(storedToken);

        // 5. Tạo token mới kế thừa family_id + session_id
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User không tồn tại."));

        return buildAuthResponse(user, storedToken.getFamilyId(), storedToken.getSessionId());
    }

    // ── LOGOUT ────────────────────────────────────────────

    /**
     * Logout đơn — revoke refresh token + blacklist access token hiện tại.
     *
     * @param refreshTokenValue refresh token raw value từ client
     * @param accessTokenValue  access token raw value từ header Authorization
     */
    @Transactional
    public void logout(String refreshTokenValue, String accessTokenValue) {
        // 1. Revoke refresh token trong DB
        String tokenHash = HashUtil.sha256Hex(refreshTokenValue);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.revoke("LOGOUT");
                    refreshTokenRepository.save(token);
                });

        // 2. Blacklist access token hiện tại → vô hiệu hóa ngay lập tức
        blacklistAccessToken(accessTokenValue, "LOGOUT");
    }

    /**
     * Logout tất cả — revoke toàn bộ refresh tokens + blacklist access token hiện tại.
     *
     * @param userId           ID của user
     * @param accessTokenValue access token raw value từ header Authorization
     */
    @Transactional
    public void logoutAll(UUID userId, String accessTokenValue) {
        // 1. Revoke tất cả refresh tokens của user
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now(), "LOGOUT_ALL");
        log.info("All sessions revoked for user {}", userId);

        // 2. Blacklist access token hiện tại
        blacklistAccessToken(accessTokenValue, "LOGOUT_ALL");
    }

    /**
     * Trích xuất thông tin từ access token và đưa vào blacklist (Redis + DB).
     *
     * @param accessTokenValue raw JWT string
     * @param reason           lý do blacklist
     */
    private void blacklistAccessToken(String accessTokenValue, String reason) {
        if (accessTokenValue == null || accessTokenValue.isBlank()) {
            log.warn("Không có access token để blacklist. Bỏ qua.");
            return;
        }

        try {
            Jwt jwt = jwtDecoder.decode(accessTokenValue);
            UUID jti = UUID.fromString(jwt.getId());
            UUID userId = UUID.fromString(jwt.getSubject());
            Instant expiresAt = jwt.getExpiresAt();

            tokenBlacklistService.blacklist(jti, userId, expiresAt, reason);
            log.info("Access token {} blacklisted (reason: {})", jti, reason);
        } catch (Exception e) {
            // Nếu decode lỗi (token hết hạn, sai format...) → bỏ qua
            // Vì token lỗi thì cũng không dùng được nữa
            log.warn("Không thể blacklist access token: {}", e.getMessage());
        }
    }

    // ── PRIVATE HELPERS ───────────────────────────────────

    /**
     * Bootstrap RBAC: gán role mặc định dựa trên system_role.
     * USER → role "member", SUPER_ADMIN → role "super_admin".
     */
    private void bootstrapUserRole(User user) {
        String roleName = user.getSystemRole() == SystemRole.SUPER_ADMIN
                ? "super_admin" : "member";
        roleRepository.findByName(roleName).ifPresent(role -> {
            UserRole userRole = UserRole.builder()
                    .userId(user.getId())
                    .role(role)
                    .scopeType(ScopeType.GLOBAL)
                    .build();
            userRoleRepository.save(userRole);
        });
    }

    /**
     * Tải tất cả permission slugs hiện tại của user (từ user_roles → role → permissions).
     * Chỉ lấy các role chưa hết hạn (isActive = true).
     */
    private Set<String> loadUserPermissions(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .filter(UserRole::isActive)
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(Permission::toSlug)
                .collect(Collectors.toSet());
    }

    /** Tạo auth response với family_id và session_id mới (dùng cho login/register). */
    private AuthResponse buildAuthResponse(User user) {
        return buildAuthResponse(user, UUID.randomUUID(), UUID.randomUUID());
    }

    /** Tạo auth response kế thừa family_id và session_id (dùng cho refresh). */
    private AuthResponse buildAuthResponse(User user, UUID familyId, UUID sessionId) {
        Set<String> permissions = loadUserPermissions(user.getId());

        // Tạo JWT access token chứa userId, email, permissions
        String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), permissions);

        // Tạo refresh token: raw value → hash → lưu DB
        String rawRefreshToken = jwtProvider.generateRefreshTokenValue();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(HashUtil.sha256Hex(rawRefreshToken))
                .familyId(familyId)
                .sessionId(sessionId)
                .expiresAt(Instant.now().plus(
                        jwtProvider.getRefreshTokenExpirationMs(), ChronoUnit.MILLIS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationMs() / 1000)
                .user(UserResponse.from(user))
                .build();
    }
}
