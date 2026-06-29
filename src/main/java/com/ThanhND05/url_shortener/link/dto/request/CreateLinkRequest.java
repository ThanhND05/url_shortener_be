package com.ThanhND05.url_shortener.link.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO tạo short link mới.
 *
 * @param originalUrl URL gốc cần rút gọn (bắt buộc).
 * @param customCode  short code tùy chọn (null → hệ thống sinh tự động).
 * @param domainId    public UUID domain (null → dùng domain mặc định).
 * @param title       tiêu đề mô tả link (tùy chọn).
 * @param description mô tả chi tiết (tùy chọn).
 * @param password    mật khẩu bảo vệ link (null → không yêu cầu mật khẩu).
 * @param redirectType HTTP redirect code: 301, 302 (mặc định), 307, 308.
 * @param startsAt    thời điểm bắt đầu hoạt động (null → ngay lập tức).
 * @param expiresAt   thời điểm hết hạn (null → vĩnh viễn).
 * @param maxClicks   giới hạn click (null → không giới hạn).
 * @param tagIds      danh sách tag IDs gắn vào link.
 */
public record CreateLinkRequest(
        @NotBlank(message = "URL gốc không được để trống")
        @URL(message = "URL không đúng định dạng")
        String originalUrl,
        String customCode,
        UUID domainId,
        String title,
        String description,
        String password,
        Short redirectType,
        Instant startsAt,
        Instant expiresAt,
        Long maxClicks,
        Set<Long> tagIds
) {}
