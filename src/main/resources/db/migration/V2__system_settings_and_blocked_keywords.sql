-- =====================================================
-- V2: Phase 3 — System Settings & Blocked Keywords
-- =====================================================

-- Bảng từ khóa bị cấm trong slug/URL
-- Admin thêm keyword → LinkService check trước khi tạo short link.
-- Ví dụ: keyword "admin" → slug "admin-panel" sẽ bị reject.
CREATE TABLE platform.blocked_keywords (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    keyword    CITEXT  NOT NULL UNIQUE,
    reason     TEXT,
    created_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_blocked_keywords_keyword ON platform.blocked_keywords(keyword);


-- Bảng cấu hình hệ thống dạng key-value
-- Admin có thể thay đổi config tại runtime mà không cần restart server.
CREATE TABLE platform.system_configs (
    config_key  VARCHAR(100)  PRIMARY KEY,
    value       TEXT          NOT NULL,
    description TEXT,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by  VARCHAR(255)
);

-- Seed default config values
INSERT INTO platform.system_configs (config_key, value, description) VALUES
    ('rate_limit.links_per_minute', '10',  'Số link tối đa user tạo mỗi phút'),
    ('rate_limit.links_per_day',    '100', 'Số link tối đa user tạo mỗi ngày'),
    ('rate_limit.api_per_minute',   '60',  'API rate limit cho 3rd party (requests/phút)'),
    ('slug.min_length',             '3',   'Độ dài tối thiểu custom slug'),
    ('slug.max_length',             '64',  'Độ dài tối đa custom slug'),
    ('link.default_expiry_days',    '0',   'Số ngày mặc định link hết hạn (0 = vĩnh viễn)'),
    ('link.max_rules_per_link',     '10',  'Số rule tối đa trên mỗi link'),
    ('user.max_links_free',         '50',  'Số link tối đa cho user FREE/tháng');
