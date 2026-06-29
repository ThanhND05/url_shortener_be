package com.ThanhND05.url_shortener.link.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO tạo routing rule cho link.
 *
 * @param ruleType  loại rule: COUNTRY, DEVICE, LANGUAGE, TIME, AB_TEST.
 * @param condition JSON điều kiện (cấu trúc phụ thuộc ruleType).
 * @param targetUrl URL đích khi điều kiện khớp.
 * @param priority  thứ tự ưu tiên (số nhỏ = ưu tiên cao).
 */
public record CreateLinkRuleRequest(
        @NotNull(message = "Rule type không được để trống")
        String ruleType,
        @NotBlank(message = "Condition không được để trống")
        String condition,
        @NotBlank(message = "Target URL không được để trống")
        String targetUrl,
        Integer priority
) {}
