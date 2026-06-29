package com.ThanhND05.url_shortener.link.entity;

import com.ThanhND05.url_shortener.link.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Entity đại diện cho bảng link.link_rules — điều kiện routing cho smart redirect.
 *
 * Cơ chế hoạt động:
 * - Mỗi link có thể có nhiều rule, sắp xếp theo priority (số nhỏ = ưu tiên cao).
 * - Khi redirect, hệ thống duyệt các rule theo priority:
 *   1. Lấy context request (country, device, language, time, ...).
 *   2. Với mỗi rule (is_active = true), check điều kiện trong `condition` (JSONB).
 *   3. Rule đầu tiên khớp → redirect tới target_url của rule đó.
 *   4. Không rule nào khớp → redirect tới original_url mặc định.
 *
 * Ví dụ condition JSONB:
 * - COUNTRY: {"countries": ["VN", "TH", "ID"]}
 * - DEVICE:  {"devices": ["mobile", "tablet"]}
 * - TIME:    {"start_hour": 9, "end_hour": 17, "timezone": "Asia/Ho_Chi_Minh"}
 * - AB_TEST: {"percentage": 50}
 */
@Entity
@Table(name = "link_rules", schema = "link")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK tới link sở hữu rule. */
    @Column(name = "link_id", nullable = false)
    private Long linkId;

    /** Thứ tự ưu tiên — số nhỏ = check trước. */
    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;

    /** Loại rule: COUNTRY, DEVICE, LANGUAGE, TIME, AB_TEST. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private RuleType ruleType;

    /** Điều kiện JSON (JSONB). Cấu trúc phụ thuộc vào rule_type. */
    @Column(name = "condition", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String condition = "{}";

    /** URL đích nếu điều kiện khớp. */
    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
