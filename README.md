<![CDATA[<div align="center">

# 🔗 URL Shortener — Backend API

**A production-grade URL shortening service built with Modular Monolith architecture**

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F.svg?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1.svg?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.2-DC382D.svg?logo=redis&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Kafka-3.6-231F20.svg?logo=apachekafka&logoColor=white)
![Google Cloud](https://img.shields.io/badge/GCP-Cloud_Run-4285F4.svg?logo=googlecloud&logoColor=white)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF.svg?logo=githubactions&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED.svg?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg)

[Architecture](#-architecture) · [Tech Stack](#-tech-stack) · [Key Features](#-key-features) · [API Reference](#-api-reference) · [Getting Started](#-getting-started) · [Deployment](#-deployment)

</div>

---

## 📖 Overview

A full-featured URL shortening platform built as a personal portfolio project, demonstrating proficiency in **Java/Spring Boot backend development**, **system design**, and **DevOps practices**. The system handles the complete lifecycle of short links — from creation and analytics tracking to subscription-based billing — all within a cleanly modularized codebase designed for future microservices migration.

### Why This Project Stands Out

| Aspect | Implementation |
|---|---|
| **Architecture** | Modular Monolith with 6 bounded modules communicating via Public API interfaces & Kafka events |
| **Performance** | Redis-cached redirects (<50ms p99 latency), Kafka-powered async click ingestion with batch INSERT (500 rows/batch) |
| **Security** | JWT (Access + Refresh tokens) with blacklist validation, BCrypt (cost=12), SHA-256 IP hashing, non-root Docker |
| **Data Design** | PostgreSQL with 6 isolated schemas, Flyway migrations, partitioned analytics tables, BRIN indexes |
| **DevOps** | Automated CI/CD pipeline: Build → Test → Trivy Scan → GHCR Push → Cloud Run Deploy |
| **Testing** | Unit + Integration tests with Testcontainers (PostgreSQL) |

---

## 🏗 Architecture

### Modular Monolith Design

The project follows a **Modular Monolith** pattern — each business domain is encapsulated in its own module with clear boundaries. Modules communicate through **Public API interfaces** (synchronous) and **Apache Kafka events** (asynchronous), minimizing coupling and enabling future extraction into independent microservices.

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway Layer                        │
│              (Spring Security + JWT Authentication)             │
├─────────┬─────────┬──────────────┬──────────┬──────────────────┤
│   IAM   │  Link   │  Analytics   │ Billing  │    Platform      │
│ Module  │ Module  │   Module     │ Module   │    Module        │
│         │         │              │          │                  │
│ • Auth  │ • CRUD  │ • Click      │ • VNPay  │ • Outbox Events  │
│ • RBAC  │ • Redir │   Tracking   │ • Subs   │ • Audit Logs     │
│ • JWT   │ • Tags  │ • Aggregates │ • Quotas │ • Blocked Lists  │
│ • Users │ • QR    │ • Counters   │          │ • System Config  │
│ • Roles │ • Rules │              │          │ • Idempotency    │
├─────────┴─────────┴──────────────┴──────────┴──────────────────┤
│                     Common / Shared Kernel                      │
│         (Security, Exception Handling, DTOs, Utilities)         │
├─────────────────────────────────────────────────────────────────┤
│  PostgreSQL (6 schemas)  │  Redis Cache  │  Apache Kafka       │
└─────────────────────────────────────────────────────────────────┘
```

### Module Responsibilities

#### 1. `iam` — Identity & Access Management
- User registration, login (email/password)
- **JWT-based authentication** with Access Token (15min) + Refresh Token (7 days)
- Token blacklisting (logout/revoke) with Redis-backed validation
- **RBAC**: Roles (`super_admin`, `admin`, `member`, `viewer`) × Permissions (`resource:action`)
- API Key management for third-party integrations

#### 2. `link` — Core URL Shortening
- Create short links with auto-generated (Base62 from PostgreSQL sequence) or custom aliases
- **Denormalized `redirect_lookup` table** for ultra-fast redirect hot path
- Domain management (custom domains with verification)
- Link rules engine (conditional redirects by country/device/language/time/A-B test)
- Tagging system (M:N relationship)
- Password-protected links, scheduled activation/expiration, max click limits
- Configurable redirect types (301, 302, 307, 308)

#### 3. `analytics` — Click Tracking & Reporting
- **Kafka consumer** with batch processing (500 events/batch → single DB round-trip)
- Real-time click tracking: IP hash, User-Agent parsing (device/OS/browser), referer analysis
- Bot detection via User-Agent pattern matching
- **Partitioned tables** (`click_events` by `occurred_at`) for time-series query performance
- Pre-aggregated daily summaries (`click_agg_daily`, `click_agg_minute`)
- Link-level counters with atomic UPSERT

#### 4. `billing` — Subscription & Payment
- **VNPay payment gateway** integration (Sandbox)
- IPN (Instant Payment Notification) webhook — server-to-server with SHA-256 signature verification
- Return URL handling with frontend redirect
- Subscription management: FREE / PRO plans with monthly link quotas
- Payment transaction history with idempotent processing

#### 5. `platform` — Cross-cutting Infrastructure
- **Transactional Outbox** pattern for reliable event publishing
- Idempotency keys for safe API retries
- Audit logging (actor, action, resource, IP hash)
- Blocked domains & keywords management
- System configuration (runtime-configurable key-value store)

#### 6. `common` — Shared Kernel
- Spring Security configuration (stateless JWT, CORS, method-level security)
- Global exception handler (`@RestControllerAdvice`) with consistent `ApiResponse<T>` format
- Pagination wrapper (`PageResponse<T>`)
- Utility classes: `Base62Encoder`, `HashUtil` (SHA-256)

### Inter-Module Communication

```
┌──────────┐    Public API Interface     ┌──────────────┐
│   Link   │ ◄──── LinkPublicApi ────►   │  Analytics   │
│  Module  │    (countLinks, getLinksByIds)│    Module    │
└────┬─────┘                             └──────────────┘
     │                                          ▲
     │  ClickEventMessage                       │
     └───────────► Kafka ───────────────────────┘
              (click-events topic)
```

- **Synchronous**: Modules expose `PublicApi` interfaces (`LinkPublicApi`, `IamPublicApi`, `AnalyticsPublicApi`) — implemented as Spring beans, consumed via dependency injection.
- **Asynchronous**: Click events flow through Kafka (`click-events` topic) from the Link module's `ClickEventProducer` to the Analytics module's `ClickEventConsumer`.

---

## 🛠 Tech Stack

### Core Backend
| Technology | Purpose |
|---|---|
| **Java 21** | Language (Virtual Threads ready, Pattern Matching, Records) |
| **Spring Boot 4.0** | Application framework |
| **Spring Security + OAuth2 Resource Server** | JWT authentication & authorization |
| **Spring Data JPA / Hibernate** | ORM with batch insert optimization (`batch_size=50`) |
| **Spring Kafka** | Async event streaming (Producer/Consumer) |
| **Spring Cache + Redis** | Distributed caching for redirect lookups |
| **Spring Validation** | Request DTO validation |
| **Flyway** | Database schema version control |
| **Lombok** | Boilerplate reduction |
| **Jackson** | JSON serialization (including `java.time` via `jackson-datatype-jsr310`) |

### Data Layer
| Technology | Purpose |
|---|---|
| **PostgreSQL 16** | Primary database with 6 isolated schemas |
| **Redis 7.2** | Cache layer (redirect lookups, JWT blacklist) |
| **Apache Kafka 3.6** | Message broker for click event pipeline |

### Testing
| Technology | Purpose |
|---|---|
| **JUnit 5** | Test framework |
| **Spring Boot Test** | Integration testing support |
| **Testcontainers** | Disposable PostgreSQL containers for integration tests |
| **MockMvc** | HTTP endpoint testing |

### DevOps & Cloud
| Technology | Purpose |
|---|---|
| **Docker** | Multi-stage build (Temurin JDK 21 → JRE 21 Alpine) |
| **GitHub Actions** | CI/CD pipeline (build → test → scan → deploy) |
| **Trivy** | Container vulnerability scanning |
| **GHCR** | Docker image registry (GitHub Container Registry) |
| **Google Cloud Run** | Serverless container deployment (auto-scaling) |
| **Cloud SQL (PostgreSQL)** | Managed relational database (private IP within VPC) |
| **Memorystore (Redis)** | Managed in-memory cache (VPC-only access, no public IP) |
| **Compute Engine** | Self-hosted Apache Kafka broker (VM instance) |
| **VPC Direct Egress** | Allows Cloud Run to reach private VPC resources (Memorystore, Kafka VM) |

---

## ⚡ Key Features

### 1. High-Performance Redirect Engine
```
Client GET /r/{shortCode}
        │
        ▼
┌─ Redis Cache Hit? ──────── YES ──► Return cached RedirectLookup
│       │ NO
│       ▼
│   Query redirect_lookup table (denormalized, indexed)
│       │
│       ▼
│   Validate: status=ACTIVE, not expired, within max_clicks, started
│       │
│       ▼
│   Publish ClickEventMessage to Kafka (async, <1ms)
│       │
│       ▼
│   HTTP 301/302/307/308 Redirect ──► Original URL
```

- **`redirect_lookup`** is a denormalized table purpose-built for the redirect hot path — avoids JOINs on the full `links` table.
- **`@Cacheable("redirects")`** caches resolved lookups in Redis, automatically evicted on link update/delete.
- Click event publishing to Kafka is non-blocking and takes <1ms, ensuring the redirect response is not delayed.

### 2. Kafka-Powered Click Analytics Pipeline
```
                     ┌──────────────────────────────────┐
                     │     ClickEventConsumer (Batch)    │
                     │                                    │
Producer ──► Kafka ──►  Poll 500 messages                │
                     │  Parse User-Agent (device/OS/bot)  │
                     │  saveAll() → Hibernate batch INSERT │
                     │  UPSERT link_counters              │
                     │  Commit Kafka offset                │
                     └──────────────────────────────────┘
```

**Before optimization**: 10,000 clicks/s → 10,000 individual INSERT statements → 10,000 DB round-trips.  
**After optimization**: 10,000 clicks/s → 20 batches × 500 rows → **20 DB round-trips (500× improvement)**.

Key design decisions:
- **At-least-once delivery**: If batch INSERT fails, Kafka offset is not committed → messages are redelivered.
- **Persistent storage**: Kafka stores messages on disk with replication — no data loss on application crash (unlike `ApplicationEventPublisher` which stores events in RAM).

### 3. Short Code Generation (Base62)
```java
PostgreSQL Sequence (100000, 100001, ...) → Base62 Encode → "q0U", "q0V", ...
```
- **Guaranteed unique**: PostgreSQL sequence is atomic — no collision checks needed.
- **Compact**: 3-5 characters for the first million links.
- **URL-safe**: `[A-Za-z0-9]` character set.

### 4. Comprehensive Security Model
- **JWT with blacklist**: Access tokens are validated against both expiry (`JwtTimestampValidator`) and a blacklist (`JwtBlacklistValidator` backed by Redis → PostgreSQL fallback).
- **Password hashing**: BCrypt with cost factor 12.
- **IP anonymization**: Client IPs are SHA-256 hashed before storage — no raw IPs in the database.
- **Non-root Docker**: Application runs as dedicated `spring` user inside the container.
- **Secrets via environment variables**: All sensitive values (DB credentials, JWT secret, VNPay keys) are externalized.

### 5. Payment Integration (VNPay)
- **IPN Webhook**: Server-to-server callback with HMAC-SHA256 signature verification.
- **Idempotent processing**: Duplicate IPN calls are safely handled.
- **Return URL**: Redirects user to frontend `/payment-result` page with transaction status.

### 6. Automated CI/CD Pipeline

```
Push to main
     │
     ▼
┌─ Build & Test ───────────────────┐
│  JDK 21 + Maven                  │
│  ./mvnw verify (Unit + IT tests) │
│  Upload test results artifact    │
└──────────────┬───────────────────┘
               │ ✅ Pass
               ▼
┌─ Docker & Push ──────────────────┐
│  Multi-stage Docker build        │
│  Trivy vulnerability scan        │
│  Push to GHCR (tagged: sha, latest)│
└──────────────┬───────────────────┘
               │ ✅ Pass
               ▼
┌─ Deploy to Cloud Run ───────────┐
│  gcloud run deploy              │
│  Direct VPC Egress (Redis/Kafka)│
│  Environment variables injected │
│  Auto-scaling enabled           │
└─────────────────────────────────┘
```

---

## 📂 Project Structure

```
src/main/java/com/ThanhND05/url_shortener/
├── UrlShortenerApplication.java
│
├── iam/                          # Identity & Access Management
│   ├── api/                      #   Public API interface for other modules
│   ├── config/                   #   Module-specific configuration
│   ├── controller/               #   AuthController, UserController, RoleController,
│   │                             #   PermissionController, ApiKeyController
│   ├── dto/                      #   Request/Response DTOs
│   ├── entity/                   #   User, Role, Permission, RefreshToken,
│   │                             #   TokenBlacklist, ApiKey, UserRole
│   ├── enums/                    #   UserStatus, etc.
│   ├── event/                    #   Domain events
│   ├── repository/               #   Spring Data JPA repositories
│   └── service/                  #   Business logic layer
│
├── link/                         # Core URL Shortening
│   ├── api/                      #   LinkPublicApi interface + impl
│   ├── controller/               #   LinkController, RedirectController,
│   │                             #   AdminLinkController, DomainController, TagController
│   ├── dto/
│   ├── entity/                   #   Link, Domain, Tag, RedirectLookup, LinkRule
│   ├── enums/                    #   LinkStatus, ShortCodeType
│   ├── event/                    #   LinkClickedEvent, LinkCreatedEvent
│   ├── repository/
│   └── service/                  #   LinkService, RedirectService,
│                                 #   ShortCodeGenerator, DomainService, TagService
│
├── analytics/                    # Click Tracking & Reporting
│   ├── api/                      #   AnalyticsPublicApi interface + impl
│   ├── controller/               #   AnalyticsController, AdminAnalyticsController
│   ├── dto/                      #   ClickEventMessage, response DTOs
│   ├── entity/                   #   ClickEvent, ClickAggDaily, ClickAggMinute,
│   │                             #   LinkCounter
│   ├── kafka/                    #   ClickEventProducer, ClickEventConsumer
│   ├── repository/
│   └── service/
│
├── billing/                      # Subscription & Payment
│   ├── controller/               #   BillingController, AdminBillingController,
│   │                             #   VnPayWebhookController
│   ├── dto/
│   ├── entity/                   #   Subscription, PaymentTransaction
│   ├── enums/                    #   SubscriptionPlan (FREE, PRO)
│   ├── listener/                 #   Event listeners
│   ├── repository/
│   └── service/
│
├── platform/                     # Cross-cutting Infrastructure
│   ├── controller/
│   ├── dto/
│   ├── entity/                   #   OutboxEvent, AuditLog, IdempotencyKey,
│   │                             #   BlockedDomain, BlockedKeyword, SystemConfig
│   ├── kafka/
│   ├── listener/
│   ├── repository/
│   └── service/
│
└── common/                       # Shared Kernel
    ├── config/                   #   SecurityConfig, RedisConfig, KafkaConfig,
    │                             #   AppProperties, JpaConfig, AsyncConfig
    ├── dto/                      #   ApiResponse<T>, PageResponse<T>
    ├── exception/                #   GlobalExceptionHandler, BusinessException,
    │                             #   ResourceNotFoundException, etc.
    ├── security/                 #   JwtProvider, JwtBlacklistValidator,
    │                             #   CustomJwtAuthenticationConverter, SecurityUtils
    └── util/                     #   Base62Encoder, HashUtil
```

---

## 📡 API Reference

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/register` | Public | Register new user |
| `POST` | `/login` | Public | Login, returns Access + Refresh tokens |
| `POST` | `/refresh` | Public | Refresh access token |
| `POST` | `/logout` | JWT | Blacklist current access token |

### Links (`/api/v1/links`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/` | JWT | Create short link |
| `GET` | `/` | JWT | List user's links (paginated) |
| `GET` | `/{publicId}` | JWT | Get link details |
| `PUT` | `/{publicId}` | JWT | Update link |
| `DELETE` | `/{publicId}` | JWT | Soft delete link |

### Redirect (`/r`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/{shortCode}` | Public | Redirect to original URL |

### Analytics (`/api/v1/analytics`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/links/{publicId}/stats` | JWT | Get click stats for a link |
| `GET` | `/links/{publicId}/daily` | JWT | Daily click breakdown |

### Billing (`/api/v1/billing`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/create-payment` | JWT | Create VNPay payment URL |
| `GET` | `/subscription` | JWT | Get current subscription |
| `GET` | `/vnpay-ipn` | Public | VNPay IPN callback (S2S) |
| `GET` | `/vnpay-return` | Public | VNPay return URL redirect |

### Admin Endpoints (`/api/v1/admin/...`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/admin/links` | Admin | List all links (system-wide) |
| `GET` | `/admin/analytics/overview` | Admin | System-wide analytics |
| `GET` | `/admin/billing/subscriptions` | Admin | All subscriptions |

### Users & RBAC (`/api/v1/users`, `/api/v1/roles`, `/api/v1/permissions`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/users/me` | JWT | Current user profile |
| `PUT` | `/users/me` | JWT | Update profile |
| `GET` | `/roles` | Admin | List all roles |
| `POST` | `/roles` | Admin | Create role |
| `GET` | `/permissions` | Admin | List all permissions |

> 📄 **Full interactive API documentation** available at `http://localhost:8080/swagger-ui/index.html` when running locally.

---

## 🗄 Database Design

### Schema Isolation

Each module owns its own PostgreSQL schema — **no cross-schema foreign keys** between bounded contexts. Inter-module references use `UUID` fields validated at the application layer.

```sql
CREATE SCHEMA IF NOT EXISTS iam;        -- Users, Roles, Permissions, Tokens
CREATE SCHEMA IF NOT EXISTS link;       -- Links, Domains, Tags, Redirect Lookup
CREATE SCHEMA IF NOT EXISTS analytics;  -- Click Events (partitioned), Aggregates
CREATE SCHEMA IF NOT EXISTS platform;   -- Outbox, Audit, Config, Blocked Lists
CREATE SCHEMA IF NOT EXISTS billing;    -- Subscriptions, Payment Transactions
```

### Key Tables (30+ tables)

| Schema | Key Tables | Notes |
|--------|-----------|-------|
| `iam` | `users`, `roles`, `permissions`, `user_roles`, `refresh_tokens`, `token_blacklist`, `api_keys` | RBAC with view `user_effective_permissions` |
| `link` | `links`, `domains`, `redirect_lookup`, `tags`, `link_tags`, `link_rules` | `redirect_lookup` = denormalized hot path |
| `analytics` | `click_events` *(partitioned)*, `click_agg_daily`, `click_agg_minute`, `link_counters` | Time-series partitioning + BRIN index |
| `platform` | `outbox_events`, `audit_logs`, `idempotency_keys`, `blocked_domains`, `blocked_keywords`, `system_configs` | Infrastructure concerns |
| `billing` | `subscriptions`, `payment_transactions` | VNPay integration |

### Performance Indexes

- **Composite index** on `(domain_id, short_code)` for redirect lookups
- **BRIN index** on `click_events.occurred_at` for time-range queries
- **Partial indexes** (e.g., `WHERE status = 'ACTIVE'`, `WHERE deleted_at IS NULL`)
- **Functional index** on `lower(name)` for case-insensitive tag uniqueness

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 21+ | Runtime |
| Docker & Docker Compose | Latest | Infrastructure services |
| Maven | 3.9+ *(or use included `mvnw`)* | Build tool |

### Local Development Setup

**1. Clone the repository:**
```bash
git clone https://github.com/ThanhND05/url_shortener_be.git
cd url_shortener_be
```

**2. Start infrastructure services:**
```bash
docker-compose up -d
```
This starts PostgreSQL, Redis, and Kafka.

**3. Run the application:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
Flyway will automatically create all schemas, tables, indexes, and seed data on first run.

**4. Verify the setup:**
```bash
# Health check
curl http://localhost:8080/actuator/health

# API docs
open http://localhost:8080/swagger-ui/index.html
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `url-shortener` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | — | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker(s) |
| `JWT_SECRET` | — | HMAC-SHA256 signing key |
| `FRONTEND_URL` | `http://localhost:3000` | Frontend base URL |
| `ALLOWED_ORIGINS` | — | CORS allowed origins |
| `VNPAY_TMN_CODE` | — | VNPay terminal code |
| `VNPAY_HASH_SECRET` | — | VNPay HMAC secret |
| `VNPAY_RETURN_URL` | — | VNPay callback URL |

### Running Tests

```bash
# All tests (unit + integration with Testcontainers)
./mvnw verify

# Unit tests only
./mvnw test
```

> **Note**: Integration tests use **Testcontainers** to spin up disposable PostgreSQL instances — Docker must be running.

---

## ☁ Deployment

### Docker Build

The project uses a **multi-stage Dockerfile** for minimal image size and enhanced security:

```dockerfile
# Stage 1: Build with JDK 21
FROM eclipse-temurin:21-jdk-alpine AS build
# ... Maven build, skip tests ...

# Stage 2: Runtime with JRE 21 only
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring  # Non-root user
USER spring
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### CI/CD Pipeline (GitHub Actions)

The pipeline runs on every push to `main` or version tag:

| Stage | Details |
|-------|---------|
| **Build & Test** | JDK 21 setup → `./mvnw verify` with Testcontainers |
| **Docker Build** | Multi-stage build with BuildKit layer caching |
| **Trivy Scan** | Container vulnerability scanning (CRITICAL severity) |
| **Push to GHCR** | Tags: `sha-<commit>`, `latest`, semver |
| **Deploy** | `gcloud run deploy` with VPC Direct Egress |

### Google Cloud Run Production Setup

```
Internet ──► Cloud Run (auto-scaling, 0→N instances)
                │
                │  ┌── VPC (default network) ──────────────────┐
                │  │                                            │
                ├──┼──► Cloud SQL          (Managed PostgreSQL) │
                ├──┼──► Memorystore        (Managed Redis)      │
                └──┼──► Compute Engine VM  (Self-hosted Kafka)  │
                   │                                            │
                   └────────────────────────────────────────────┘
```

- **VPC Direct Egress** (`--vpc-egress private-ranges-only`): Cloud Run is serverless and sits **outside** the VPC by default. This flag routes traffic destined for private IP ranges through the VPC, enabling access to Memorystore (which has **no public IP**), Cloud SQL (via private IP), and the Kafka VM.
- **Auto-scaling**: Cloud Run scales from 0 to N instances based on traffic.
- **Resource allocation**: 2 vCPU, 1 GiB RAM per instance.

---

## 🧪 Testing Strategy

| Layer | Scope | Tools |
|-------|-------|-------|
| **Unit Tests** | Service logic, utilities (Base62, hashing) | JUnit 5, Mockito |
| **Integration Tests** | Full HTTP flow (create link → redirect → verify) | Spring Boot Test, Testcontainers, MockMvc |

Test files:
- `ShortCodeGeneratorTest` — Base62 encoding correctness
- `Base62EncoderTest` — Encoder/decoder verification
- `BillingServiceTest` — Subscription & payment logic
- `VnPayServiceTest` — VNPay signature verification
- `LinkCreationIntegrationTest` — End-to-end link creation via API
- `RedirectIntegrationTest` — End-to-end redirect flow with DB

---

## 🔮 Future Improvements

- [ ] **Rate Limiting**: Token-bucket rate limiter (Spring Cloud Gateway or Bucket4j)
- [ ] **GeoIP Lookup**: MaxMind GeoLite2 integration for country/city analytics
- [ ] **QR Code Generation**: Dynamic QR code with custom branding
- [ ] **Observability Stack**: Prometheus + Grafana + Distributed Tracing (Micrometer)
- [ ] **Microservices Extraction**: Split modules into independent deployable services
- [ ] **WebSocket Dashboard**: Real-time analytics with live click stream
- [ ] **Link Preview / OG Tags**: Fetch and display Open Graph metadata

---

> This is a personal portfolio project built to demonstrate proficiency in modern backend architecture, cloud-native deployment, and production-grade engineering practices.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
]]>
