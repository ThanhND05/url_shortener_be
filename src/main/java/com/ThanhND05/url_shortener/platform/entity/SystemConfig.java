package com.ThanhND05.url_shortener.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity đại diện cho bảng platform.system_configs — cấu hình hệ thống dạng key-value.
 *
 * Cho phép admin thay đổi config tại runtime mà không cần restart server.
 * Ví dụ:
 *   - rate_limit.links_per_minute = 10
 *   - slug.min_length = 3
 *   - link.default_expiry_days = 0
 *
 * Service layer đọc config từ DB, cache trong Redis/memory để giảm query.
 */
@Entity
@Table(name = "system_configs", schema = "platform")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    /** Config key, đóng vai trò primary key (VD: "rate_limit.links_per_minute"). */
    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    /** Giá trị config (lưu dưới dạng text, parse tùy context). */
    @Column(nullable = false)
    private String value;

    /** Mô tả ý nghĩa config. */
    @Column(name = "description")
    private String description;

    /** Thời điểm cập nhật gần nhất. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /** Email admin cập nhật gần nhất. */
    @Column(name = "updated_by")
    private String updatedBy;
}
