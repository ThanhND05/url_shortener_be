-- ====================================================================
-- V2: Tạo bảng iam.token_blacklist
-- Lưu JTI của access token đã bị vô hiệu hóa (logout, đổi mật khẩu, ...)
-- Entry tự động được cleanup sau khi token hết hạn tự nhiên.
-- ====================================================================

CREATE TABLE IF NOT EXISTS iam.token_blacklist (
    jti         UUID            PRIMARY KEY,
    user_id     UUID            NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ     NOT NULL,
    reason      VARCHAR(80),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Index để cleanup job xóa các entry đã hết hạn
CREATE INDEX idx_token_blacklist_expires_at ON iam.token_blacklist(expires_at);

-- Index để tìm tất cả blacklisted tokens của 1 user (dùng cho logout-all)
CREATE INDEX idx_token_blacklist_user_id ON iam.token_blacklist(user_id);
