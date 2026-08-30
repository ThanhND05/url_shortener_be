-- =========================================================
-- V1: Initial Database Schema
-- URL Shortener System
-- =========================================================

-- =========================================================
-- Extensions
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

-- =========================================================
-- Schemas
-- =========================================================

CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS link;
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS platform;
CREATE SCHEMA IF NOT EXISTS billing;

-- =========================================================
-- IAM
-- =========================================================

CREATE TABLE iam.users (
id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
email           CITEXT      NOT NULL UNIQUE,
password_hash   TEXT        NOT NULL,
display_name    VARCHAR(150),
avatar_url      TEXT,
status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
CHECK (status IN ('ACTIVE', 'LOCKED', 'DELETED')),
deleted_at      TIMESTAMPTZ,
created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_users_status
ON iam.users(status)
WHERE status != 'DELETED';

CREATE INDEX ix_users_deleted_at
ON iam.users(deleted_at)
WHERE deleted_at IS NOT NULL;

CREATE INDEX ix_users_created_at
ON iam.users(created_at DESC);

---

-- Refresh Tokens

---

CREATE TABLE iam.refresh_tokens (
id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
user_id         UUID        NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
token_hash      TEXT        NOT NULL UNIQUE,
family_id       UUID        NOT NULL DEFAULT gen_random_uuid(),
session_id      UUID        NOT NULL DEFAULT gen_random_uuid(),
device_name     VARCHAR(150),
ip_hash         BYTEA,
user_agent_hash BYTEA,
expires_at      TIMESTAMPTZ NOT NULL,
revoked_at      TIMESTAMPTZ,
revoke_reason   VARCHAR(80),
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_refresh_tokens_user_id
ON iam.refresh_tokens(user_id);

CREATE INDEX ix_refresh_tokens_session_id
ON iam.refresh_tokens(session_id);

CREATE INDEX ix_refresh_tokens_family_id
ON iam.refresh_tokens(family_id);

CREATE INDEX ix_refresh_tokens_expires_at
ON iam.refresh_tokens(expires_at)
WHERE revoked_at IS NULL;

---

-- Token Blacklist

---

CREATE TABLE IF NOT EXISTS iam.token_blacklist (
jti         UUID            PRIMARY KEY,
user_id     UUID            NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
expires_at  TIMESTAMPTZ     NOT NULL,
reason      VARCHAR(80)
);

CREATE INDEX IF NOT EXISTS idx_token_blacklist_expires_at
ON iam.token_blacklist(expires_at);

CREATE INDEX IF NOT EXISTS idx_token_blacklist_user_id
ON iam.token_blacklist(user_id);

---

-- Roles

---

CREATE TABLE iam.roles (
id           BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
name         VARCHAR(80) NOT NULL UNIQUE,
display_name VARCHAR(150),
description  TEXT,
is_system    BOOLEAN     NOT NULL DEFAULT false,
created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO iam.roles (name, display_name, is_system) VALUES
('super_admin', 'Super Admin', true),
('admin',       'Admin',       true),
('member',      'Member',      true),
('viewer',      'Viewer',      true);

---

-- Permissions

---

CREATE TABLE iam.permissions (
id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
resource    VARCHAR(80) NOT NULL,
action      VARCHAR(80) NOT NULL,
description TEXT,
created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
UNIQUE (resource, action)
);

CREATE INDEX ix_permissions_resource
ON iam.permissions(resource);

INSERT INTO iam.permissions (resource, action) VALUES
('link',      'create'),
('link',      'read'),
('link',      'update'),
('link',      'delete'),
('link',      'export'),
('domain',    'create'),
('domain',    'read'),
('domain',    'update'),
('domain',    'delete'),
('analytics', 'read'),
('analytics', 'export'),
('user',      'read'),
('user',      'manage'),
('billing',   'read'),
('billing',   'manage');

---

-- Role Permissions

---

CREATE TABLE iam.role_permissions (
role_id       BIGINT NOT NULL REFERENCES iam.roles(id)       ON DELETE CASCADE,
permission_id BIGINT NOT NULL REFERENCES iam.permissions(id) ON DELETE CASCADE,
PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX ix_role_permissions_permission_id
ON iam.role_permissions(permission_id);

-- Super Admin
INSERT INTO iam.role_permissions (role_id, permission_id)
SELECT 1, id
FROM iam.permissions
ON CONFLICT DO NOTHING;

-- Admin: tất cả trừ user và billing
INSERT INTO iam.role_permissions (role_id, permission_id)
SELECT 2, id
FROM iam.permissions
WHERE resource NOT IN ('user', 'billing')
ON CONFLICT DO NOTHING;

-- Member
INSERT INTO iam.role_permissions (role_id, permission_id) VALUES
(3, 1),
(3, 2),
(3, 3),
(3, 4),
(3, 6),
(3, 7),
(3, 10)
ON CONFLICT DO NOTHING;

-- Viewer
INSERT INTO iam.role_permissions (role_id, permission_id) VALUES
(4, 2),
(4, 7),
(4, 10)
ON CONFLICT DO NOTHING;

---

-- User Roles

---

CREATE TABLE iam.user_roles (
id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
user_id    UUID        NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
role_id    BIGINT      NOT NULL REFERENCES iam.roles(id) ON DELETE CASCADE,
scope_type VARCHAR(30) NOT NULL DEFAULT 'GLOBAL'
CHECK (scope_type IN ('GLOBAL', 'WORKSPACE')),
scope_id   UUID,
granted_by UUID        REFERENCES iam.users(id) ON DELETE SET NULL,
granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
expires_at TIMESTAMPTZ,
UNIQUE (user_id, role_id, scope_type, scope_id)
);

CREATE INDEX ix_user_roles_user_id
ON iam.user_roles(user_id);

CREATE INDEX ix_user_roles_role_id
ON iam.user_roles(role_id);

CREATE INDEX ix_user_roles_scope
ON iam.user_roles(scope_type, scope_id)
WHERE scope_id IS NOT NULL;

---

-- API Keys

---

CREATE TABLE iam.api_keys (
id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
owner_id              UUID         NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
name                  VARCHAR(150) NOT NULL,
key_prefix            VARCHAR(20)  NOT NULL,
key_hash              TEXT         NOT NULL UNIQUE,
scopes                TEXT[]       NOT NULL DEFAULT '{}',
rate_limit_per_minute INT          NOT NULL DEFAULT 60,
status                VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE'
CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
revoke_reason         TEXT,
expires_at            TIMESTAMPTZ,
last_used_at          TIMESTAMPTZ,
created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_api_keys_owner_id
ON iam.api_keys(owner_id);

CREATE INDEX ix_api_keys_key_hash
ON iam.api_keys(key_hash);

CREATE INDEX ix_api_keys_status
ON iam.api_keys(status)
WHERE status = 'ACTIVE';

---

-- Effective Permissions View

---

CREATE VIEW iam.user_effective_permissions AS
SELECT
ur.user_id,
ur.scope_type,
ur.scope_id,
p.resource,
p.action,
p.resource || ':' || p.action AS permission_slug
FROM iam.user_roles ur
JOIN iam.role_permissions rp ON rp.role_id = ur.role_id
JOIN iam.permissions p       ON p.id = rp.permission_id
WHERE ur.expires_at IS NULL
OR ur.expires_at > now();

-- =========================================================
-- LINK
-- =========================================================

CREATE TABLE link.domains (
id                 BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id          UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),

```
-- Cross-schema reference to iam.users is intentionally
-- application-managed to reduce database coupling.
owner_id           UUID,

domain             CITEXT      NOT NULL UNIQUE,
is_default         BOOLEAN     NOT NULL DEFAULT false,
status             VARCHAR(30) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED', 'DELETED')),
verification_token TEXT,
verified_at        TIMESTAMPTZ,
created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
```

);

CREATE INDEX ix_domains_owner_id
ON link.domains(owner_id);

CREATE INDEX ix_domains_status
ON link.domains(status);

CREATE UNIQUE INDEX ux_domains_owner_default
ON link.domains(owner_id)
WHERE is_default = true;

---

-- Short Code Sequence

---

CREATE SEQUENCE link.short_code_seq
START WITH 100000
INCREMENT BY 1;

-- System default domain
INSERT INTO link.domains (domain, is_default, status, verified_at)
VALUES (
'api-url-shortener.thanhnd.vn',
true,
'ACTIVE',
now()
)
ON CONFLICT DO NOTHING;

---

-- Links

---

CREATE TABLE link.links (
id                         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
public_id                  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),

```
-- Cross-schema reference to iam.users is intentionally
-- application-managed to reduce database coupling.
owner_id                   UUID,

domain_id                  BIGINT      NOT NULL REFERENCES link.domains(id),

short_code                 VARCHAR(64) NOT NULL,
short_code_type            VARCHAR(30) NOT NULL DEFAULT 'GENERATED'
    CHECK (short_code_type IN ('GENERATED', 'CUSTOM')),

original_url               TEXT        NOT NULL,
normalized_url             TEXT,
original_url_hash          VARCHAR(64),
title                      VARCHAR(255),
description                TEXT,

status                     VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN (
        'ACTIVE',
        'DISABLED',
        'EXPIRED',
        'DELETED',
        'QUARANTINED'
    )),

redirect_type              SMALLINT    NOT NULL DEFAULT 302
    CHECK (redirect_type IN (301, 302, 307, 308)),

starts_at                  TIMESTAMPTZ,
expires_at                 TIMESTAMPTZ,
max_clicks                 BIGINT,
click_count                BIGINT      NOT NULL DEFAULT 0,
last_clicked_at            TIMESTAMPTZ,

password_hash              TEXT,
metadata                   JSONB       NOT NULL DEFAULT '{}',

created_by_ip_hash         BYTEA,
created_by_user_agent_hash BYTEA,

deleted_at                 TIMESTAMPTZ,
created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),

CONSTRAINT ck_links_short_code_format
    CHECK (short_code ~ '^[A-Za-z0-9_-]{3,64}$'),

CONSTRAINT ck_links_date_range
    CHECK (
        starts_at IS NULL
        OR expires_at IS NULL
        OR starts_at < expires_at
    )
```

);

CREATE UNIQUE INDEX ux_links_domain_short_code
ON link.links(domain_id, short_code);

CREATE INDEX ix_links_owner_created_at
ON link.links(owner_id, created_at DESC);

CREATE INDEX ix_links_status
ON link.links(status);

CREATE INDEX ix_links_expires_at
ON link.links(expires_at);

CREATE INDEX ix_links_original_url_hash
ON link.links(original_url_hash);

CREATE INDEX ix_links_active_redirect
ON link.links(domain_id, short_code, status)
WHERE deleted_at IS NULL;

---

-- Redirect Lookup

---

CREATE TABLE link.redirect_lookup (
domain_id         BIGINT      NOT NULL,
short_code        VARCHAR(64) NOT NULL,
link_id           BIGINT      NOT NULL,
link_public_id    UUID        NOT NULL,
original_url      TEXT        NOT NULL,
status            VARCHAR(30) NOT NULL,
redirect_type     SMALLINT    NOT NULL DEFAULT 302,
starts_at         TIMESTAMPTZ,
expires_at        TIMESTAMPTZ,
max_clicks        BIGINT,
click_count       BIGINT      NOT NULL DEFAULT 0,
password_required BOOLEAN     NOT NULL DEFAULT false,
updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
PRIMARY KEY (domain_id, short_code)
);

CREATE INDEX ix_redirect_lookup_status
ON link.redirect_lookup(status);

CREATE INDEX ix_redirect_lookup_expires_at
ON link.redirect_lookup(expires_at);

---

-- Link Rules

---

CREATE TABLE link.link_rules (
id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
link_id    BIGINT      NOT NULL REFERENCES link.links(id) ON DELETE CASCADE,
priority   INT         NOT NULL DEFAULT 100,
rule_type  VARCHAR(30) NOT NULL
CHECK (rule_type IN (
'COUNTRY',
'DEVICE',
'LANGUAGE',
'TIME',
'AB_TEST'
)),
condition  JSONB       NOT NULL DEFAULT '{}',
target_url TEXT        NOT NULL,
is_active  BOOLEAN     NOT NULL DEFAULT true,
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_link_rules_link_id
ON link.link_rules(link_id, priority);

---

-- Tags

---

CREATE TABLE link.tags (
id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

```
-- Cross-schema reference to iam.users is intentionally
-- application-managed to reduce database coupling.
owner_id   UUID        NOT NULL,

name       VARCHAR(80) NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
```

);

CREATE UNIQUE INDEX ux_tags_owner_name
ON link.tags(owner_id, lower(name));

---

-- Link Tags

---

CREATE TABLE link.link_tags (
link_id BIGINT NOT NULL REFERENCES link.links(id) ON DELETE CASCADE,
tag_id  BIGINT NOT NULL REFERENCES link.tags(id)  ON DELETE CASCADE,
PRIMARY KEY (link_id, tag_id)
);

-- =========================================================
-- ANALYTICS
-- =========================================================

CREATE TABLE IF NOT EXISTS analytics.click_events (
event_id                   UUID            NOT NULL DEFAULT gen_random_uuid(),
link_id                    BIGINT,
link_public_id             UUID,
domain_id                  BIGINT,
short_code                 VARCHAR(64),
occurred_at                TIMESTAMPTZ     NOT NULL DEFAULT now(),
ip_hash                    BYTEA,
visitor_hash               BYTEA,
user_agent_hash            BYTEA,
referer                    TEXT,
referer_domain             VARCHAR(255),
country_code               VARCHAR(2),
region                     VARCHAR(100),
city                       VARCHAR(100),
device_type                VARCHAR(30),
os                         VARCHAR(80),
browser                    VARCHAR(80),
is_bot                     BOOLEAN         NOT NULL DEFAULT false,
request_id                 UUID,
http_status                SMALLINT,
latency_ms                 INTEGER,
metadata                   JSONB           NOT NULL DEFAULT '{}'::jsonb,

```
CONSTRAINT click_events_pkey
    PRIMARY KEY (occurred_at, event_id)
```

) PARTITION BY RANGE (occurred_at);

CREATE INDEX IF NOT EXISTS ix_click_events_code_time
ON analytics.click_events (
domain_id,
short_code,
occurred_at DESC NULLS FIRST
);

CREATE INDEX IF NOT EXISTS ix_click_events_link_time
ON analytics.click_events (
link_id,
occurred_at DESC NULLS FIRST
);

CREATE INDEX IF NOT EXISTS ix_click_events_time_brin
ON analytics.click_events USING BRIN (occurred_at);

CREATE INDEX IF NOT EXISTS ix_click_events_country
ON analytics.click_events(country_code);

CREATE INDEX IF NOT EXISTS ix_click_events_device
ON analytics.click_events(device_type);

---

-- Click Aggregation - Minute

---

CREATE TABLE IF NOT EXISTS analytics.click_agg_minute (
link_id          BIGINT                      NOT NULL,
bucket_minute    TIMESTAMP WITH TIME ZONE    NOT NULL,
total_clicks     BIGINT                      NOT NULL DEFAULT 0,
unique_visitors  BIGINT                      NOT NULL DEFAULT 0,
bot_clicks       BIGINT                      NOT NULL DEFAULT 0,
country_counts   JSONB                       NOT NULL DEFAULT '{}'::jsonb,
device_counts    JSONB                       NOT NULL DEFAULT '{}'::jsonb,
referrer_counts  JSONB                       NOT NULL DEFAULT '{}'::jsonb,
updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),

```
CONSTRAINT click_agg_minute_pkey
    PRIMARY KEY (link_id, bucket_minute)
```

) PARTITION BY RANGE (bucket_minute);

CREATE INDEX IF NOT EXISTS ix_click_agg_minute_bucket
ON analytics.click_agg_minute (
bucket_minute DESC NULLS FIRST
);

---

-- Click Aggregation - Daily

---

CREATE TABLE analytics.click_agg_daily (
link_id         BIGINT NOT NULL,
day             DATE   NOT NULL,
total_clicks    BIGINT NOT NULL DEFAULT 0,
unique_visitors BIGINT NOT NULL DEFAULT 0,
bot_clicks      BIGINT NOT NULL DEFAULT 0,
country_counts  JSONB  NOT NULL DEFAULT '{}',
device_counts   JSONB  NOT NULL DEFAULT '{}',
referrer_counts JSONB  NOT NULL DEFAULT '{}',
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
PRIMARY KEY (link_id, day)
);

CREATE INDEX ix_click_agg_daily_day
ON analytics.click_agg_daily(day DESC);

---

-- Link Counters

---

CREATE TABLE analytics.link_counters (
link_id                   BIGINT      PRIMARY KEY,
total_clicks              BIGINT      NOT NULL DEFAULT 0,
unique_visitors_estimate  BIGINT      NOT NULL DEFAULT 0,
last_clicked_at           TIMESTAMPTZ,
updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_link_counters_top
ON analytics.link_counters(total_clicks DESC);

-- =========================================================
-- PLATFORM
-- =========================================================

---

-- Outbox Events

---

CREATE TABLE platform.outbox_events (
id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
aggregate_type VARCHAR(100) NOT NULL,
aggregate_id   VARCHAR(100) NOT NULL,
event_type     VARCHAR(150) NOT NULL,
payload        JSONB        NOT NULL,
created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
published_at   TIMESTAMPTZ,
retry_count    INT          NOT NULL DEFAULT 0,
last_error     TEXT
);

CREATE INDEX ix_outbox_events_unpublished
ON platform.outbox_events(created_at)
WHERE published_at IS NULL;

CREATE INDEX ix_outbox_events_aggregate
ON platform.outbox_events(aggregate_type, aggregate_id);

---

-- Idempotency Keys

---

CREATE TABLE platform.idempotency_keys (
-- Cross-schema user reference is application-managed.
owner_id        UUID         NOT NULL,

```
idempotency_key VARCHAR(120) NOT NULL,
request_hash    VARCHAR(64)  NOT NULL,
response_status INT,
response_body   JSONB,
created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
expires_at      TIMESTAMPTZ  NOT NULL,

PRIMARY KEY (owner_id, idempotency_key)
```

);

CREATE INDEX ix_idempotency_keys_expires_at
ON platform.idempotency_keys(expires_at);

---

-- Audit Logs

---

CREATE TABLE platform.audit_logs (
id              BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

```
-- Cross-schema reference to iam.users is application-managed.
actor_id        UUID,

action          VARCHAR(120) NOT NULL,
resource_type   VARCHAR(100),
resource_id     VARCHAR(100),
ip_hash         BYTEA,
user_agent_hash BYTEA,
metadata        JSONB       NOT NULL DEFAULT '{}',
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
```

);

CREATE INDEX ix_audit_logs_actor_time
ON platform.audit_logs(actor_id, created_at DESC);

CREATE INDEX ix_audit_logs_resource
ON platform.audit_logs(resource_type, resource_id);

---

-- Blocked Domains

---

CREATE TABLE platform.blocked_domains (
id         BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
domain     CITEXT  NOT NULL UNIQUE,
reason     TEXT,
source     VARCHAR(100),
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

---

-- Blocked Keywords

---

-- Admin thêm keyword → LinkService check trước khi tạo short link.
-- Ví dụ: keyword "admin" → slug "admin-panel" sẽ bị reject.

CREATE TABLE platform.blocked_keywords (
id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
keyword    CITEXT NOT NULL UNIQUE,
reason     TEXT,
created_by VARCHAR(255),
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_blocked_keywords_keyword
ON platform.blocked_keywords(keyword);

---

-- System Configs

---

-- Cấu hình hệ thống dạng key-value.
-- Admin có thể thay đổi config tại runtime
-- mà không cần restart server.

CREATE TABLE platform.system_configs (
config_key  VARCHAR(100) PRIMARY KEY,
value       TEXT         NOT NULL,
description TEXT,
updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
updated_by  VARCHAR(255)
);

-- Default configuration values
INSERT INTO platform.system_configs
(config_key, value, description)
VALUES
(
'rate_limit.links_per_minute',
'10',
'Số link tối đa user tạo mỗi phút'
),
(
'rate_limit.links_per_day',
'100',
'Số link tối đa user tạo mỗi ngày'
),
(
'rate_limit.api_per_minute',
'60',
'API rate limit cho 3rd party (requests/phút)'
),
(
'slug.min_length',
'3',
'Độ dài tối thiểu custom slug'
),
(
'slug.max_length',
'64',
'Độ dài tối đa custom slug'
),
(
'link.default_expiry_days',
'0',
'Số ngày mặc định link hết hạn (0 = vĩnh viễn)'
),
(
'link.max_rules_per_link',
'10',
'Số rule tối đa trên mỗi link'
),
(
'user.max_links_free',
'50',
'Số link tối đa cho user FREE/tháng'
);

-- =========================================================
-- BILLING
-- =========================================================

---

-- Subscriptions

---

-- Mỗi user chỉ có 1 subscription.
-- user_id không FK sang iam.users để tránh database coupling
-- giữa IAM và Billing.

CREATE TABLE billing.subscriptions (
user_id         UUID            PRIMARY KEY,

```
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
```

);

---

-- Payment Transactions

---

-- Lịch sử giao dịch thanh toán VNPay.
-- user_id được kiểm tra ở Application Layer.

CREATE TABLE billing.payment_transactions (
id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
user_id           UUID         NOT NULL,

```
txn_ref           VARCHAR(100)  NOT NULL UNIQUE,
amount            BIGINT        NOT NULL,
order_info        TEXT,
vnp_txn_no        VARCHAR(100),
vnp_response_code VARCHAR(10),
vnp_bank_code     VARCHAR(30),
vnp_pay_date      VARCHAR(30),

status            VARCHAR(30)   NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),

created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
```

);

CREATE INDEX idx_payment_transactions_user_id
ON billing.payment_transactions(user_id);

CREATE INDEX idx_payment_transactions_status
ON billing.payment_transactions(status);

CREATE INDEX idx_payment_transactions_txn_ref
ON billing.payment_transactions(txn_ref);

---

-- Billing Permissions

---

-- billing:read = permission_id 14
-- billing:manage = permission_id 15
-- member = role_id 3

INSERT INTO iam.role_permissions (role_id, permission_id) VALUES
(3, 14),
(3, 15)
ON CONFLICT DO NOTHING;

-- =========================================================
-- END OF V1
-- =========================================================
