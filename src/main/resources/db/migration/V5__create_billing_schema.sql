-- =========================================================
-- V5: Tạo schema BILLING — Quản lý gói dịch vụ & thanh toán VNPay
-- =========================================================

CREATE SCHEMA IF NOT EXISTS billing;

-- ---------------------------------------------------------
-- Bảng subscriptions: Gói dịch vụ hiện tại của mỗi user
-- Mỗi user chỉ có 1 bản ghi (1-1 với iam.users).
-- Mặc định khi tạo user: plan = FREE, không có expires_at.
-- Khi thanh toán thành công: plan = PRO, expires_at = +30 ngày.
-- ---------------------------------------------------------
CREATE TABLE billing.subscriptions (
    user_id         UUID            PRIMARY KEY REFERENCES iam.users(id) ON DELETE CASCADE,
    plan            VARCHAR(30)     NOT NULL DEFAULT 'FREE'
        CHECK (plan IN ('FREE', 'PRO')),
    status          VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    started_at      TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    links_used      INT             NOT NULL DEFAULT 0,
    links_reset_at  TIMESTAMPTZ     NOT NULL DEFAULT date_trunc('month', now()),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);


-- ---------------------------------------------------------
-- Bảng payment_transactions: Lịch sử giao dịch thanh toán VNPay
-- Mỗi lần user bấm "Nâng cấp Pro" sẽ tạo 1 bản ghi PENDING.
-- VNPay IPN callback sẽ cập nhật status → SUCCESS / FAILED.
-- ---------------------------------------------------------
CREATE TABLE billing.payment_transactions (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    txn_ref         VARCHAR(100)    NOT NULL UNIQUE,
    amount          BIGINT          NOT NULL,
    order_info      TEXT,
    vnp_txn_no      VARCHAR(100),
    vnp_response_code VARCHAR(10),
    vnp_bank_code   VARCHAR(30),
    vnp_pay_date    VARCHAR(30),
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_transactions_user_id ON billing.payment_transactions(user_id);
CREATE INDEX idx_payment_transactions_status  ON billing.payment_transactions(status);
CREATE INDEX idx_payment_transactions_txn_ref ON billing.payment_transactions(txn_ref);
