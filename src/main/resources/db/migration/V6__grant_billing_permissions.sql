-- =========================================================
-- V6: Gán quyền billing cho Member role
-- Member cần billing:read (xem gói) và billing:manage (thanh toán)
-- =========================================================

-- billing:read = permission_id 14, billing:manage = permission_id 15
-- member = role_id 3
INSERT INTO iam.role_permissions (role_id, permission_id) VALUES
    (3, 14),  -- billing:read
    (3, 15)   -- billing:manage
ON CONFLICT DO NOTHING;
