package com.ThanhND05.url_shortener.iam.enums;

/**
 * Phạm vi áp dụng của một role khi gán cho user.
 *
 * - GLOBAL:    quyền áp dụng toàn hệ thống (scope_id = NULL).
 * - WORKSPACE: quyền giới hạn trong một workspace cụ thể (scope_id = UUID của workspace).
 *
 * Ví dụ: user A có role "admin" với scope WORKSPACE + scope_id = workspace-xyz
 *        → user A chỉ là admin trong workspace-xyz, không phải toàn hệ thống.
 */
public enum ScopeType {
    GLOBAL,
    WORKSPACE
}
