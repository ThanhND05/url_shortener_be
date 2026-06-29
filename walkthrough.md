# URL Shortener — Walkthrough Phase 1, 2, 3, 4 & 5

## Tổng quan kiến trúc

Hệ thống sử dụng kiến trúc **Modular Monolith** — một ứng dụng Spring Boot duy nhất, nhưng code được tổ chức theo module nghiệp vụ (package-by-feature). Mỗi module tương ứng với một PostgreSQL schema:

```
com.ThanhND05.url_shortener/
├── common/    → Shared kernel (config, security, DTOs, exceptions, utils)
├── iam/       → Identity & Access Management (schema: iam)
├── link/      → Link management (schema: link) — Phase 3
├── analytics/ → Click analytics (schema: analytics) — Phase 4
└── platform/  → Infrastructure (schema: platform) — Phase 5
```

---

## Phase 1: Foundation (`common/`)

### 1.1 Flyway Migration
**File:** [V1__init_schema.sql](file:///d:/url-shortener/src/main/resources/db/migration/V1__init_schema.sql)

- Chứa toàn bộ SQL tạo 4 schemas + tất cả bảng, index, view, seed data.
- Flyway tự động chạy khi app khởi động, kiểm tra version → chỉ chạy migration chưa apply.
- Hibernate `ddl-auto: validate` sẽ kiểm tra entity mapping khớp với DB schema.

### 1.2 Configuration

| File | Chức năng |
|------|-----------|
| [application.yaml](file:///d:/url-shortener/src/main/resources/application.yaml) | DataSource (PostgreSQL), Flyway, Redis, JWT secret, CORS |
| [AppProperties.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/config/AppProperties.java) | Bind `app.jwt.*` và `app.cors.*` vào Java objects |
| [JpaConfig.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/config/JpaConfig.java) | JPA Auditing — tự fill `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy` (UUID từ JWT) |
| [SecurityConfig.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/config/SecurityConfig.java) | Security filter chain, JWT decoder/encoder, BCrypt, CORS |
| [RedisConfig.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/config/RedisConfig.java) | EnableCaching (hiện dùng in-memory, chuyển Redis khi có) |
| [AsyncConfig.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/config/AsyncConfig.java) | EnableAsync + EnableScheduling cho xử lý bất đồng bộ |

### 1.3 Security Architecture

```
Client Request
  │
  ├─ Header: "Authorization: Bearer <JWT>"
  │
  ▼
SecurityFilterChain (SecurityConfig)
  │
  ├─ /api/v1/auth/**  → permitAll() (không cần token)
  ├─ /r/**             → permitAll() (redirect public)
  ├─ /actuator/health  → permitAll()
  └─ anyRequest()      → authenticated()
       │
       ▼
  OAuth2 Resource Server (auto)
       │
       ├─ Extract Bearer token
       ├─ JwtDecoder (HMAC-SHA256) → verify signature + expiry
       └─ CustomJwtAuthenticationConverter
            │
            └─ Extract "permissions" claim → GrantedAuthority list
                 VD: ["link:create", "analytics:read"]
                      → SimpleGrantedAuthority("link:create"), ...
```

**Các file security:**

| File | Vai trò |
|------|---------|
| [JwtProvider.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/security/JwtProvider.java) | Tạo JWT access token (chứa userId, email, permissions trong claims) |
| [CustomJwtAuthenticationConverter.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/security/CustomJwtAuthenticationConverter.java) | Parse JWT → extract `permissions` claim → map thành Spring Security authorities |
| [SecurityUtils.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/security/SecurityUtils.java) | Helper để lấy userId, email, JTI từ SecurityContext trong service layer |

### 1.4 DTOs & Exception Handling

| File | Mục đích |
|------|----------|
| [ApiResponse.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/dto/ApiResponse.java) | Wrapper chuẩn cho mọi API response: `{ success, data, message, timestamp }` |
| [PageResponse.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/dto/PageResponse.java) | Wrapper cho response có phân trang (từ Spring `Page`) |
| [GlobalExceptionHandler.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/exception/GlobalExceptionHandler.java) | Bắt tất cả exception → trả ApiResponse với HTTP status phù hợp |

**Exception mapping:**
- `ResourceNotFoundException` → 404
- `DuplicateResourceException` → 409
- `BusinessException` → 400
- `UnauthorizedException` → 401
- `AccessDeniedException` → 403
- `MethodArgumentNotValidException` → 400 + chi tiết từng field lỗi

### 1.5 Utilities

| File | Chức năng |
|------|-----------|
| [HashUtil.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/util/HashUtil.java) | SHA-256 hash → hex string (cho URL, API key) hoặc byte[] (cho IP, user-agent lưu BYTEA) |
| [Base62Encoder.java](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/common/util/Base62Encoder.java) | Encode số từ sequence sang chuỗi ngắn URL-safe (VD: 100000 → "q0U") |

---

## Phase 2: Module IAM (`iam/`)

### 2.1 Entities — Mapping tới PostgreSQL schema `iam`

| Entity | Bảng DB | Mô tả |
|--------|---------|-------|
| [User](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/iam/entity/User.java) | `iam.users` | Tài khoản user — email, password hash (BCrypt), soft-delete qua `deleted_at` |
| [RefreshToken](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/iam/entity/RefreshToken.java) | `iam.refresh_tokens` | Refresh token đã hash — family rotation, session tracking |
| [TokenBlacklist](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/iam/entity/TokenBlacklist.java) | `iam.token_blacklist` | Blacklist JTI của access token khi cần revoke khẩn cấp |
| [Role](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/iam/entity/Role.java) | `iam.roles` | Vai trò (M:N → permissions qua `role_permissions`) |
| [Permission](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/iam/entity/Permission.java) | `iam.permissions` | Quyền dạng `resource:action` (VD: "link:create") |
| [UserRole](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/iam/entity/UserRole.java) | `iam.user_roles` | Gán role cho user với scope (GLOBAL / WORKSPACE) + expiry |
| [ApiKey](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/iam/entity/ApiKey.java) | `iam.api_keys` | API key cho programmatic access — lưu hash, scopes, rate limit |

### 2.2 Flow Xác Thực (Authentication)

#### Đăng ký (`POST /api/v1/auth/register`)
```
Client gửi: { email, password, displayName }
  │
  ▼
AuthService.register()
  ├─ Check email chưa tồn tại
  ├─ BCrypt.encode(password) → lưu User
  ├─ Bootstrap RBAC: gán role "member" mặc định
  ├─ Publish UserCreatedEvent
  └─ Tạo cặp token:
       ├─ Access Token (JWT, 15 phút): { sub: userId, email, permissions: [...] }
       └─ Refresh Token (UUID random):
            ├─ SHA-256(rawToken) → lưu vào DB
            └─ Trả rawToken cho client
```

#### Đăng nhập (`POST /api/v1/auth/login`)
```
Client gửi: { email, password }
  │
  ▼
AuthService.login()
  ├─ Tìm user bằng email
  ├─ Check status == ACTIVE
  ├─ BCrypt.matches(password, hash) → verify
  ├─ Load permissions: user_roles → role → permissions → slugs
  └─ Tạo cặp token (như register)
```

#### Refresh Token (`POST /api/v1/auth/refresh`)
```
Client gửi: { refreshToken: "raw-uuid-string" }
  │
  ▼
AuthService.refresh()
  ├─ SHA-256(rawToken) → tìm trong DB
  ├─ Nếu đã bị revoke:
  │    └─ ⚠️ TOKEN THEFT DETECTED!
  │         Revoke TOÀN BỘ family → user phải login lại
  ├─ Nếu hết hạn → reject
  └─ Nếu hợp lệ:
       ├─ Revoke token cũ (reason: "ROTATED")
       └─ Tạo token mới KẾ THỪA family_id + session_id
            (tiếp tục chuỗi rotation)
```

> **Family Rotation giải thích:**
> Mỗi lần login tạo ra một `family_id` mới. Khi refresh, token mới giữ cùng `family_id`.
> Nếu attacker đánh cắp refresh token cũ và dùng lại → server phát hiện token đã revoke
> nhưng cùng family → revoke TẤT CẢ tokens trong family → cả user lẫn attacker đều bị logout.

### 2.3 Phân quyền (Authorization — RBAC)

```
User ──M:N──▶ UserRole ──M:1──▶ Role ──M:N──▶ Permission
                 │                                │
                 ├─ scope (GLOBAL/WORKSPACE)      └─ slug: "link:create"
                 └─ expires_at (có thể tạm thời)
```

**Flow kiểm tra quyền:**
1. Khi login: load tất cả permissions → nhúng vào JWT claim `"permissions"`.
2. Mỗi request: `CustomJwtAuthenticationConverter` parse claim → `GrantedAuthority`.
3. Controller dùng `@PreAuthorize("hasAuthority('link:create')")` để check.

**Seed data (migration):** 4 system roles + 15 permissions đã được INSERT sẵn.

### 2.4 API Key Flow

```
1. User tạo key:
   POST /api/v1/api-keys { name: "Production", scopes: ["link:create"] }
   
   Server:
   ├─ Sinh: raw = "sk_live_a1b2c3d4..."
   ├─ Lưu: prefix = "sk_live_a1b2c3d4...", hash = SHA-256(raw)
   └─ Trả: { rawKey: "sk_live_a1b2c3d4..." } ← LẦN DUY NHẤT

2. Client dùng key (implement ở phase sau):
   Header: X-API-Key: sk_live_a1b2c3d4...
   Server: hash → lookup → check status + scopes → authenticate
```

### 2.5 DTOs (Data Transfer Objects)

| DTO | Dùng ở đâu | Mô tả |
|-----|------------|-------|
| `RegisterRequest` | Auth | email + password + displayName, có validation |
| `LoginRequest` | Auth | email + password |
| `AuthResponse` | Auth | accessToken + refreshToken + user info |
| `RefreshTokenRequest` | Auth | chứa refreshToken string |
| `UserResponse` | User | thông tin user (KHÔNG chứa password) |
| `UpdateProfileRequest` | User | displayName + avatarUrl |
| `ChangePasswordRequest` | User | currentPassword + newPassword |
| `RoleResponse` | Role | role info + set permission slugs |
| `AssignRoleRequest` | Role | roleName + scopeType + scopeId |
| `CreateApiKeyRequest` | ApiKey | name + scopes + expiresAt |
| `ApiKeyResponse` | ApiKey | key info + rawKey (chỉ lần tạo) |

### 2.6 Events (Inter-module Communication)

| Event | Publisher | Mục đích |
|-------|-----------|----------|
| `UserCreatedEvent` | AuthService | Audit log, welcome email (future) |
| `PasswordChangedEvent` | UserService | Revoke sessions, audit log |
| `AccountLockedEvent` | UserService | Revoke sessions, audit log |

Các event dùng Spring `ApplicationEventPublisher` — hiện chưa có listener (sẽ implement ở Platform module, Phase 5).

### 2.7 API Endpoints Tổng hợp

#### Auth (Public — không cần token)
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/auth/register` | Đăng ký |
| POST | `/api/v1/auth/login` | Đăng nhập |
| POST | `/api/v1/auth/refresh` | Refresh token |
| POST | `/api/v1/auth/logout` | Logout (cần token) |
| POST | `/api/v1/auth/logout-all` | Logout tất cả (cần token) |

#### User (Authenticated)
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/users/me` | Xem profile |
| PUT | `/api/v1/users/me` | Sửa profile |
| PUT | `/api/v1/users/me/password` | Đổi mật khẩu |
| GET | `/api/v1/users` | Danh sách user (admin) |
| PUT | `/api/v1/users/{id}/lock` | Khóa user (admin) |
| PUT | `/api/v1/users/{id}/unlock` | Mở khóa (admin) |

#### Roles (Admin — cần permission `user:manage`)
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/roles` | Liệt kê roles |
| POST | `/api/v1/roles` | Tạo custom role |
| PUT | `/api/v1/roles/{id}/permissions` | Cập nhật permissions |
| POST | `/api/v1/roles/users/{userId}/assign` | Gán role |
| GET | `/api/v1/roles/users/{userId}/permissions` | Xem permissions |

#### API Keys (Authenticated)
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/api-keys` | Tạo API key |
| GET | `/api/v1/api-keys` | Liệt kê keys |
| DELETE | `/api/v1/api-keys/{id}` | Thu hồi key |

## Phase 3: Module Link (`link/`)

### 3.1 Entities — Mapping tới PostgreSQL schema `link`

| Entity | Bảng DB | Mô tả |
|--------|---------|-------|
| [Domain](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/entity/Domain.java) | `link.domains` | Custom domain — xác minh DNS, mỗi user có 1 domain mặc định |
| [Link](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/entity/Link.java) | `link.links` | Short link đầy đủ — URL, scheduling, password, M:N tags |
| [RedirectLookup](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/entity/RedirectLookup.java) | `link.redirect_lookup` | Bảng denormalized cho hot-path redirect (PK: domain_id + short_code) |
| [RedirectLookupId](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/entity/RedirectLookupId.java) | — | Composite PK class cho RedirectLookup |
| [LinkRule](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/entity/LinkRule.java) | `link.link_rules` | Routing rule (COUNTRY, DEVICE, AB_TEST, ...) |
| [Tag](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/entity/Tag.java) | `link.tags` | Nhãn phân loại link, scoped per user |

### 3.2 Flow Tạo Short Link (`POST /api/v1/links`)

```
Client gửi: { originalUrl, customCode?, domainId?, password?, tags, ... }
  │
  ▼
LinkService.createLink()
  ├─ 1. Resolve domain (custom theo publicId, hoặc default của user)
  ├─ 2. Sinh short code:
  │      ├─ customCode != null → validate regex + check trùng
  │      └─ null → ShortCodeGenerator:
  │           ├─ SELECT nextval('link.short_code_seq')  (VD: 100000)
  │           └─ Base62Encoder.encode(100000) → "q0U"
  ├─ 3. Hash originalUrl (SHA-256) → detect link trùng
  ├─ 4. Hash password (BCrypt) nếu có
  ├─ 5. Gắn tags → lưu Link entity
  ├─ 6. Sync → redirect_lookup (denormalized copy cho hot path)
  └─ 7. Publish LinkCreatedEvent → audit log
```

### 3.3 Flow Redirect — Hot Path (`GET /r/{shortCode}`)

```
Client truy cập: GET /r/q0U
  │
  ▼
RedirectController.redirect()
  ├─ Extract client info: IP, User-Agent, Referer
  │
  ▼
RedirectService.processRedirect()
  ├─ 1. resolve(shortCode):
  │      ├─ @Cacheable("redirects") → check Redis/in-memory cache
  │      └─ Cache miss → query redirect_lookup WHERE short_code = 'q0U' AND status = 'ACTIVE'
  │
  ├─ 2. Validate:
  │      ├─ starts_at chưa đến → 400 "Link chưa kích hoạt"
  │      ├─ expires_at đã qua → 400 "Link hết hạn"
  │      ├─ click_count ≥ max_clicks → 400 "Đạt giới hạn"
  │      └─ password_required → 400 "Cần mật khẩu"
  │
  ├─ 3. Publish LinkClickedEvent (async):
  │      └─ { linkId, domainId, ipHash, userAgent, referer }
  │         → Analytics module (Phase 4) sẽ nhận và xử lý
  │
  └─ 4. Return original_url + redirect_type
         │
         ▼
RedirectController → HTTP 301/302/307/308
  └─ Header: Location: https://original-url.com
```

> **Tại sao cần redirect_lookup (denormalized)?**
> - Bảng `links` có nhiều cột + join tags → query nặng.
> - `redirect_lookup` chỉ chứa đúng các trường cần → query nhẹ, dễ cache.
> - App sync data từ links → redirect_lookup mỗi khi INSERT/UPDATE.
> - Không dùng DB trigger — app layer kiểm soát logic.

### 3.4 Quản lý Domain

| File | Chức năng |
|------|-----------|
| [DomainService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/service/DomainService.java) | CRUD + verify + set default |
| [DomainController](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/link/controller/DomainController.java) | REST API cho domain management |

Flow verify (MVP): set status = ACTIVE ngay lập tức (DNS check sẽ implement sau).

### 3.5 Routing Rules (Smart Redirect)

Mỗi link có thể có nhiều routing rule:
```
Link "go.site.vn/sale"
  ├─ Rule 1 (priority=1, COUNTRY): {"countries":["VN"]} → https://vn.shop.com
  ├─ Rule 2 (priority=2, DEVICE): {"devices":["mobile"]} → https://m.shop.com
  └─ Default (no match) → https://shop.com
```

### 3.6 DTOs

**Request** (`link/dto/request/`):
| DTO | Mô tả |
|-----|-------|
| `CreateDomainRequest` | domain name |
| `CreateLinkRequest` | originalUrl, customCode, password, tags, scheduling |
| `UpdateLinkRequest` | partial update (null = giữ nguyên) |
| `CreateLinkRuleRequest` | ruleType + condition JSON + targetUrl |
| `CreateTagRequest` | tag name |

**Response** (`link/dto/response/`):
| DTO | Mô tả |
|-----|-------|
| `DomainResponse` | domain info + verification status |
| `LinkResponse` | full link info + tag names + click count |
| `LinkRuleResponse` | rule details |
| `TagResponse` | tag id + name |

### 3.7 Events

| Event | Publisher | Listener (future) |
|-------|-----------|-------------------|
| `LinkCreatedEvent` | LinkService | Platform → audit log |
| `LinkClickedEvent` | RedirectService | Analytics → click tracking |

### 3.8 API Endpoints — Link Module

#### Domains (Authenticated, cần permission `domain:*`)
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/domains` | Đăng ký domain |
| GET | `/api/v1/domains` | Danh sách domains |
| PUT | `/api/v1/domains/{id}/verify` | Xác minh domain |
| PUT | `/api/v1/domains/{id}/default` | Đặt default |
| DELETE | `/api/v1/domains/{id}` | Xóa domain |

#### Links (Authenticated, cần permission `link:*`)
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/links` | Tạo short link |
| GET | `/api/v1/links` | Danh sách links |
| GET | `/api/v1/links/{publicId}` | Chi tiết link |
| PUT | `/api/v1/links/{publicId}` | Cập nhật link |
| DELETE | `/api/v1/links/{publicId}` | Xóa link |
| POST | `/api/v1/links/{publicId}/rules` | Thêm routing rule |
| GET | `/api/v1/links/{publicId}/rules` | Xem rules |

#### Tags (Authenticated)
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/api/v1/tags` | Tạo tag |
| GET | `/api/v1/tags` | Danh sách tags |
| DELETE | `/api/v1/tags/{id}` | Xóa tag |

#### Redirect (Public — không cần token)
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/r/{shortCode}` | Redirect → original URL |

---

## Phase 4: Module Analytics (`analytics/`)

### 4.1 Tổng quan kiến trúc Analytics

```
LinkClickedEvent (từ RedirectService)
  │
  ▼
ClickIngestionService (@Async @EventListener)
  ├─ 1. Parse User-Agent → device_type, os, browser, is_bot
  ├─ 2. Tính visitor_hash = SHA-256(ip + ua) → unique visitor
  ├─ 3. Trích xuất referer_domain từ Referer URL
  ├─ 4. INSERT → analytics.click_events (partitioned table)
  └─ 5. UPSERT → analytics.link_counters (atomic increment)

Scheduled Jobs:
  ├─ [mỗi 60s] AggregationService.minuteAggregation()
  │      click_events → GROUP BY link, trunc(minute)
  │      → UPSERT analytics.click_agg_minute
  │
  └─ [00:05 hàng ngày] AggregationService.dailyAggregation()
         click_agg_minute → SUM by link, day
         → UPSERT analytics.click_agg_daily

Query Layer:
  AnalyticsQueryService → AnalyticsController
  ├─ /stats       → link_counters + timeseries 24h
  ├─ /timeseries  → auto-granularity (≤24h → minute, >24h → daily)
  └─ /clicks      → raw click_events (phân trang)
```

### 4.2 Entities — Mapping tới PostgreSQL schema `analytics`

| Entity | Bảng DB | Mô tả |
|--------|---------|-------|
| [ClickEvent](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/entity/ClickEvent.java) | `analytics.click_events` | Raw click event — partitioned by `occurred_at` (1 partition/tháng) |
| [ClickEventId](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/entity/ClickEventId.java) | — | Composite PK (occurred_at, event_id) — bắt buộc cho partitioned table |
| [ClickAggMinute](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/entity/ClickAggMinute.java) | `analytics.click_agg_minute` | Click gộp theo phút — real-time dashboard, giữ 7 ngày |
| [ClickAggDaily](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/entity/ClickAggDaily.java) | `analytics.click_agg_daily` | Click gộp theo ngày — long-term analytics, giữ vĩnh viễn |
| [LinkCounter](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/entity/LinkCounter.java) | `analytics.link_counters` | Counter tổng hợp per link — total clicks, unique visitors, last click |

### 4.3 Flow Ingestion — Từ Click → Analytics

```
User click GET /r/q0U
  │
  ├─ RedirectService → return 302 (KHÔNG block)
  │
  └─ publish LinkClickedEvent (async)
       │
       ▼
ClickIngestionService.handleClickEvent()
  ├─ Parse User-Agent (regex đơn giản):
  │    ├─ deviceType: "mobile" / "tablet" / "desktop"
  │    ├─ os: "Windows" / "macOS" / "Android" / "iOS" / "Linux"
  │    ├─ browser: "Chrome" / "Firefox" / "Safari" / "Edge" / "Opera"
  │    └─ isBot: check pattern "bot|crawl|spider|..."
  │
  ├─ visitorHash = SHA-256(ip_hash_hex + userAgent)
  │    → xấp xỉ unique visitor (cùng IP + UA = cùng visitor)
  │
  ├─ refererDomain = extractDomain(referer)
  │    VD: "https://www.facebook.com/post/123" → "facebook.com"
  │
  ├─ INSERT analytics.click_events
  │    (partitioned: tự route vào partition tháng hiện tại)
  │
  └─ UPSERT analytics.link_counters
       INSERT ... ON CONFLICT (link_id) DO UPDATE
       SET total_clicks = total_clicks + 1, last_clicked_at = now()
```

> **Tại sao @Async?**
> Redirect response trả cho client NGAY LẬP TỨC (< 5ms).
> Analytics ghi nhận chạy trên thread pool riêng — nếu fail cũng không ảnh hưởng redirect.

### 4.4 Aggregation Pipeline

```
┌──────────────┐   mỗi 60s    ┌──────────────────┐   00:05 daily   ┌────────────────┐
│ click_events │ ──────────▶  │ click_agg_minute │ ──────────────▶ │ click_agg_daily│
│ (raw, ~M rows)│              │ (1440 rows/link/ │                 │ (1 row/link/day)│
│ partition/    │              │  ngày, giữ 7d)   │                 │ giữ vĩnh viễn  │
│ tháng         │              └──────────────────┘                 └────────────────┘
└──────────────┘
                               GROUP BY link_id,                    SUM(total_clicks),
                               trunc(occurred_at, min)              SUM(unique_visitors)
                               COUNT(DISTINCT visitor_hash)
```

**Breakdown counts (JSONB):** Mỗi aggregate row chứa 3 JSONB columns:
- `country_counts`: `{"VN": 120, "US": 45}`
- `device_counts`: `{"mobile": 100, "desktop": 80}`
- `referrer_counts`: `{"facebook.com": 50, "direct": 25}`

### 4.5 Services

| Service | File | Chức năng |
|---------|------|-----------|
| [ClickIngestionService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/service/ClickIngestionService.java) | Listener | Nhận LinkClickedEvent → parse UA → INSERT click_events + UPSERT counters |
| [AggregationService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/service/AggregationService.java) | Scheduled | Job mỗi 60s (minute agg) + 00:05 daily (daily agg) |
| [AnalyticsQueryService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/analytics/service/AnalyticsQueryService.java) | Query | Stats, auto-granularity timeseries, click log |

### 4.6 DTOs

**Response** (`analytics/dto/response/`):
| DTO | Mô tả |
|-----|-------|
| `ClickEventResponse` | Chi tiết 1 click (country, device, browser, referer, isBot) |
| `ClickAggResponse` | 1 data point trong timeseries (bucket, total, unique, breakdowns) |
| `LinkStatsResponse` | Tổng hợp: counters + timeseries list |

### 4.7 API Endpoints — Analytics Module

#### Analytics (Authenticated, cần permission `analytics:read`)
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/analytics/links/{publicId}/stats` | Counters + timeseries 24h |
| GET | `/api/v1/analytics/links/{publicId}/timeseries?from=...&to=...` | Timeseries custom range (auto-granularity) |
| GET | `/api/v1/analytics/links/{publicId}/clicks?from=...&to=...` | Raw click log (phân trang) |

**Auto-granularity logic:**
- `to - from ≤ 24h` → trả data từ `click_agg_minute` (tối đa 1440 points).
- `to - from > 24h` → trả data từ `click_agg_daily` (VD: 90 points cho 90 ngày).

---

## Phase 5: Module Platform (`platform/`)

### 5.1 Tổng quan — Infrastructure Services

Module Platform cung cấp các dịch vụ nền tảng dùng chung cho toàn bộ hệ thống:

```
                    ┌─────────────────────────────────┐
                    │        Platform Module           │
                    ├─────────────┬───────────────────┤
                    │ AuditService│ OutboxService      │
                    │ (nhật ký)   │ (reliable events)  │
                    ├─────────────┼───────────────────┤
                    │ Idempotency │ BlockedDomain      │
                    │ Service     │ Service             │
                    │ (chống dup) │ (URL blacklist)    │
                    └─────────────┴───────────────────┘
                           ▲              ▲
                    Events │     Direct   │
                    (async)│     call     │
               ┌───────────┘              └──────────┐
               │                                     │
          IAM Module                          Link Module
    UserCreated, PasswordChanged         isBlocked(url)
    AccountLocked, LinkCreated
```

### 5.2 Entities — Mapping tới PostgreSQL schema `platform`

| Entity | Bảng DB | Mô tả |
|--------|---------|-------|
| [OutboxEvent](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/entity/OutboxEvent.java) | `platform.outbox_events` | Transactional Outbox — đảm bảo at-least-once event delivery |
| [IdempotencyKey](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/entity/IdempotencyKey.java) | `platform.idempotency_keys` | Chống duplicate request (header `Idempotency-Key`) |
| [AuditLog](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/entity/AuditLog.java) | `platform.audit_logs` | Nhật ký hành động: ai, làm gì, lúc nào |
| [BlockedDomain](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/entity/BlockedDomain.java) | `platform.blocked_domains` | Blacklist domain phishing/malware |

### 5.3 Audit Log — Flow Ghi Nhật Ký

```
IAM Module publish UserCreatedEvent
  │
  ▼
AuditEventListener (@Async @EventListener)    ← Platform module
  │
  ├─ onUserCreated()     → AuditService.logAction("USER_CREATED")
  ├─ onPasswordChanged() → AuditService.logAction("PASSWORD_CHANGED")
  ├─ onAccountLocked()   → AuditService.logAction("ACCOUNT_LOCKED")
  └─ onLinkCreated()     → AuditService.logAction("LINK_CREATED")
       │
       ▼
  INSERT platform.audit_logs
  { actor_id, action, resource_type, resource_id, metadata }
```

> **Tại sao @Async?** Audit logging KHÔNG block business logic. Nếu ghi log fail, operation chính vẫn thành công.

### 5.4 Transactional Outbox Pattern

```
Business Transaction:
  ┌─────────────────────────────────────────┐
  │ linkRepository.save(link);              │
  │ outboxService.addEvent(                 │
  │   "Link", publicId, "LinkCreated",     │  ← CÙNG TRANSACTION
  │   payloadJson);                         │
  └─────────────────────────────────────────┘
          ↓ commit cả 2 cùng lúc

Poller (mỗi 5 giây):
  ┌─────────────────────────────────────────┐
  │ 1. SELECT * FROM outbox_events          │
  │    WHERE published_at IS NULL           │
  │ 2. Gửi event (MVP: log, prod: Kafka)   │
  │ 3. SET published_at = now()             │
  │ 4. Nếu fail: retry_count++, last_error │
  │ 5. retry ≥ 5 → skip (dead letter)      │
  └─────────────────────────────────────────┘
```

### 5.5 Idempotency — Chống Duplicate Request

```
Client → POST /api/v1/links  Header: Idempotency-Key: abc-123
  │
  ▼
IdempotencyService.check(userId, "abc-123")
  ├─ Key tồn tại + body hash khớp → trả cached response (KHÔNG xử lý lại)
  ├─ Key tồn tại + body hash KHÁC → 409 Conflict
  └─ Key chưa tồn tại → xử lý bình thường → save key + response

Scheduled cleanup (mỗi giờ): xóa keys hết hạn (TTL 24h)
```

### 5.6 Blocked Domains — URL Blacklist

```
Link Module: LinkService.createLink()
  │
  ├─ blockedDomainService.isBlocked(originalUrl)
  │    ├─ Extract host từ URL
  │    ├─ Loại bỏ "www." prefix
  │    └─ Check bảng platform.blocked_domains
  │
  ├─ true  → reject: "Domain bị chặn"
  └─ false → tiếp tục tạo link
```

### 5.7 Services

| Service | File | Chức năng |
|---------|------|-----------|
| [AuditService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/service/AuditService.java) | Core | Ghi + query audit logs |
| [OutboxService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/service/OutboxService.java) | Core | Outbox pattern: addEvent() + pollAndPublish() scheduled |
| [IdempotencyService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/service/IdempotencyService.java) | Core | Check/save idempotency keys + cleanup expired |
| [BlockedDomainService](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/service/BlockedDomainService.java) | Cross-module | CRUD blacklist + isBlocked() cho Link module |

### 5.8 Listener

| Listener | File | Events |
|----------|------|--------|
| [AuditEventListener](file:///d:/url-shortener/src/main/java/com/ThanhND05/url_shortener/platform/listener/AuditEventListener.java) | Catch-all | UserCreated, PasswordChanged, AccountLocked, LinkCreated |

### 5.9 DTOs

**Request** (`platform/dto/request/`):
| DTO | Mô tả |
|-----|-------|
| `CreateBlockedDomainRequest` | domain + reason + source |

**Response** (`platform/dto/response/`):
| DTO | Mô tả |
|-----|-------|
| `AuditLogResponse` | id, actor, action, resource, metadata, createdAt |
| `BlockedDomainResponse` | id, domain, reason, source, createdAt |

### 5.10 API Endpoints — Platform Module

#### Audit Logs (Admin, cần permission `user:manage`)
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/audit-logs` | Tất cả audit logs (phân trang) |
| GET | `/api/v1/audit-logs/users/{userId}` | Logs theo user |

#### Blocked Domains (Admin, cần permission `user:manage`)
| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/v1/blocked-domains` | Danh sách domains bị chặn |
| POST | `/api/v1/blocked-domains` | Thêm domain vào blacklist |
| DELETE | `/api/v1/blocked-domains/{id}` | Xóa domain khỏi blacklist |

---

## Verification

- ✅ Phase 1 + 2: BUILD SUCCESS (58 source files)
- ✅ Phase 1 + 2 + 3: BUILD SUCCESS (93 source files)
- ✅ Phase 1 + 2 + 3 + 4: BUILD SUCCESS (111 source files)
- ✅ Phase 1 + 2 + 3 + 4 + 5: BUILD SUCCESS (130 source files)
- Flyway migration sẽ chạy khi start app với PostgreSQL



