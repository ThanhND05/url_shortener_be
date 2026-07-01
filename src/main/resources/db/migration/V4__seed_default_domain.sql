-- Tạo một system domain mặc định để hệ thống có thể tạo link rút gọn
INSERT INTO link.domains (domain, is_default, status, verified_at)
VALUES ('localhost:8080', true, 'ACTIVE', now())
ON CONFLICT DO NOTHING;
