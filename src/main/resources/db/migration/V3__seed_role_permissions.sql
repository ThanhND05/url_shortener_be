-- Gán toàn bộ 15 quyền (từ 1 đến 15) cho Super Admin (role_id = 1)
INSERT INTO iam.role_permissions (role_id, permission_id)
SELECT 1, id FROM iam.permissions
ON CONFLICT DO NOTHING;

-- Gán quyền cho Admin (role_id = 2) - Gán tất cả trừ quản lý user và billing
INSERT INTO iam.role_permissions (role_id, permission_id)
SELECT 2, id FROM iam.permissions 
WHERE resource NOT IN ('user', 'billing')
ON CONFLICT DO NOTHING;

-- Gán quyền cho Member (role_id = 3) - Được tạo, sửa, xóa link, domain và xem thống kê
INSERT INTO iam.role_permissions (role_id, permission_id) VALUES
    (3, 1),  -- link:create
    (3, 2),  -- link:read
    (3, 3),  -- link:update
    (3, 4),  -- link:delete
    (3, 6),  -- domain:create
    (3, 7),  -- domain:read
    (3, 10)  -- analytics:read
ON CONFLICT DO NOTHING;

-- Gán quyền cho Viewer (role_id = 4) - Chỉ được xem
INSERT INTO iam.role_permissions (role_id, permission_id) VALUES
    (4, 2),  -- link:read
    (4, 7),  -- domain:read
    (4, 10)  -- analytics:read
ON CONFLICT DO NOTHING;
