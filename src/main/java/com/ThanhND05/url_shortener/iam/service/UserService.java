package com.ThanhND05.url_shortener.iam.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.entity.User;
import com.ThanhND05.url_shortener.iam.enums.UserStatus;
import com.ThanhND05.url_shortener.iam.event.AccountLockedEvent;
import com.ThanhND05.url_shortener.iam.event.PasswordChangedEvent;
import com.ThanhND05.url_shortener.iam.repository.RefreshTokenRepository;
import com.ThanhND05.url_shortener.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service quản lý người dùng: xem/sửa profile, đổi mật khẩu, khóa/mở tài khoản.
 *
 * === ĐỔI MẬT KHẨU ===
 * 1. Verify mật khẩu cũ bằng BCrypt.
 * 2. Hash mật khẩu mới → cập nhật DB.
 * 3. Publish PasswordChangedEvent → revoke tất cả sessions + blacklist access tokens.
 * → Đảm bảo tất cả thiết bị phải đăng nhập lại với mật khẩu mới.
 *
 * === KHÓA TÀI KHOẢN (ADMIN) ===
 * 1. Set status = LOCKED.
 * 2. Publish AccountLockedEvent → revoke tất cả sessions.
 * → User bị khóa không thể dùng token cũ, không thể đăng nhập.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // ── XEM PROFILE ───────────────────────────────────────

    /** Lấy thông tin user hiện tại (từ JWT subject). */
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = findActiveUser(userId);
        return UserResponse.from(user);
    }

    // ── CẬP NHẬT PROFILE ─────────────────────────────────

    /** Cập nhật tên hiển thị và/hoặc avatar. Chỉ cập nhật field non-null. */
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    // ── ĐỔI MẬT KHẨU ────────────────────────────────────

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        // Verify mật khẩu cũ
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }

        // Hash và lưu mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke tất cả refresh tokens ngay lập tức
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now(), "PASSWORD_CHANGE");

        // Publish event → các listener khác xử lý (blacklist access tokens, audit log)
        eventPublisher.publishEvent(new PasswordChangedEvent(userId));
        log.info("Password changed for user {}", userId);
    }

    // ── ADMIN: DANH SÁCH USER ─────────────────────────────

    /** Lấy danh sách user (trừ DELETED) với phân trang — chỉ admin dùng. */
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findByStatusNot(UserStatus.DELETED, pageable)
                .map(UserResponse::from);
    }

    // ── ADMIN: KHÓA / MỞ KHÓA ────────────────────────────

    /** Khóa tài khoản user → revoke sessions + publish event. */
    @Transactional
    public UserResponse lockUser(UUID userId) {
        User user = findActiveUser(userId);
        user.setStatus(UserStatus.LOCKED);
        user = userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId, Instant.now(), "ACCOUNT_LOCKED");
        eventPublisher.publishEvent(new AccountLockedEvent(userId, "Locked by admin"));
        log.info("User locked: {}", userId);
        return UserResponse.from(user);
    }

    /** Mở khóa tài khoản — user có thể đăng nhập lại. */
    @Transactional
    public UserResponse unlockUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getStatus() != UserStatus.LOCKED) {
            throw new BusinessException("Chỉ có thể mở khóa tài khoản đang bị khóa.");
        }
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        log.info("User unlocked: {}", userId);
        return UserResponse.from(user);
    }

    // ── HELPER ────────────────────────────────────────────

    private User findActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return user;
    }
}
