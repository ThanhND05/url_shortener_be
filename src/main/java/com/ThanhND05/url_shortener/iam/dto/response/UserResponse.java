package com.ThanhND05.url_shortener.iam.dto.response;

import com.ThanhND05.url_shortener.iam.entity.User;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi thông tin user — KHÔNG bao giờ chứa passwordHash.
 * Dùng factory method from(User) để convert từ entity.
 */
@Builder
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String status,
        Instant createdAt
) {
    /** Convert từ User entity sang DTO — loại bỏ thông tin nhạy cảm. */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
