<div align="center">

# 🔗 URL Shortener — Backend API

**A production-grade URL shortening service built with Modular Monolith architecture**

Link web: https://url-shortener-thanh-nd.vercel.app/

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F.svg?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1.svg?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.2-DC382D.svg?logo=redis&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Kafka-3.6-231F20.svg?logo=apachekafka&logoColor=white)
![Google Cloud](https://img.shields.io/badge/GCP-Cloud_Run-4285F4.svg?logo=googlecloud&logoColor=white)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF.svg?logo=githubactions&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED.svg?logo=docker&logoColor=white)

[Architecture](#-architecture) · [Tech Stack](#-tech-stack) · [Key Features](#-key-features) · [Getting Started](#-getting-started) · [Deployment](#-deployment)

</div>

---

## 📖 Overview

A full-featured URL shortening platform demonstrating proficiency in **Java/Spring Boot backend development**, **system design**, and **DevOps practices**. The system handles the complete lifecycle of short links — from creation and analytics tracking to subscription-based billing — all within a cleanly modularized codebase designed for future microservices migration.

| Aspect | Implementation |
|---|---|
| **Architecture** | Modular Monolith with 6 bounded modules communicating via Public API interfaces & Kafka events |
| **Performance** | Redis-cached redirects, Kafka-powered async click ingestion with batch INSERT (500 rows/batch) |
| **Security** | JWT (Access + Refresh tokens) with blacklist validation, BCrypt (cost=12), SHA-256 IP hashing, non-root Docker |
| **Data Design** | PostgreSQL with 6 isolated schemas, Flyway migrations, partitioned analytics tables, BRIN indexes |
| **DevOps** | Automated CI/CD: Build → Test → Trivy Scan → GHCR Push → Cloud Run Deploy |
| **Testing** | Unit + Integration tests with Testcontainers (PostgreSQL) |

---

## 🏗 Architecture

The project follows a **Modular Monolith** pattern — each business domain is encapsulated in its own module with clear boundaries. Modules communicate through **Public API interfaces** (synchronous) and **Apache Kafka events** (asynchronous), minimizing coupling and enabling future microservices extraction.

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
│ • Users │ • Rules │ • Counters   │          │ • System Config  │
│ • Roles │         │              │          │ • Idempotency    │
├─────────┴─────────┴──────────────┴──────────┴──────────────────┤
│                     Common / Shared Kernel                      │
│         (Security, Exception Handling, DTOs, Utilities)         │
├─────────────────────────────────────────────────────────────────┤
│  PostgreSQL (6 schemas)  │  Redis Cache  │  Apache Kafka       │
└─────────────────────────────────────────────────────────────────┘
```

### Module Overview

| Module | Responsibilities |
|---|---|
| **`iam`** | User registration/login, JWT auth (Access + Refresh tokens), Token blacklisting, RBAC (4 roles × 15 permissions), API Key management |
| **`link`** | Short link CRUD, Base62 code generation (PostgreSQL sequence), Denormalized `redirect_lookup` table, Domain management, Link rules engine, Tags, Password protection, Scheduled expiry |
| **`analytics`** | Kafka batch consumer (500 events/batch), Click tracking (IP hash, User-Agent parsing, referer), Bot detection, Partitioned `click_events` table, Pre-aggregated daily/minute summaries |
| **`billing`** | VNPay payment gateway (IPN webhook + HMAC-SHA512 verification), Subscription management (FREE/PRO), Monthly link quotas, Idempotent transaction processing |
| **`platform`** | Transactional Outbox pattern, Idempotency keys, Audit logging, Blocked domains/keywords, Runtime system configuration |
| **`common`** | Spring Security config, Global exception handler, `ApiResponse<T>` / `PageResponse<T>`, Base62Encoder, HashUtil |

### Inter-Module Communication

- **Synchronous**: Modules expose `PublicApi` interfaces (`LinkPublicApi`, `IamPublicApi`, `AnalyticsPublicApi`) — Spring beans consumed via DI.
- **Asynchronous**: Click events flow through Kafka (`click-events` topic) — `ClickEventProducer` → `ClickEventConsumer` (batch mode).

---

## 🛠 Tech Stack

| Category | Technologies |
|---|---|
| **Core** | Java 21, Spring Boot 4.0, Spring Security + OAuth2 Resource Server, Spring Data JPA/Hibernate, Spring Kafka, Spring Cache, Flyway, Lombok |
| **Data** | PostgreSQL 16 (6 schemas), Redis 7.2 (cache + JWT blacklist), Apache Kafka 3.6 (click events) |
| **Testing** | JUnit 5, Testcontainers (PostgreSQL), Spring Boot Test, MockMvc |
| **DevOps** | Docker (multi-stage), GitHub Actions, Trivy (vulnerability scan), GHCR |
| **Cloud (GCP)** | Cloud Run (serverless), Cloud SQL (managed PostgreSQL), Memorystore (managed Redis), Compute Engine (Kafka VM), VPC Direct Egress |

---

## ⚡ Key Features

### 1. High-Performance Redirect Engine

```
Client GET /r/{shortCode}
     │
     ▼
Redis Cache Hit? ── YES ──► Return cached RedirectLookup
     │ NO
     ▼
Query redirect_lookup (denormalized, indexed)
     │
     ▼
Validate (status, expiry, max_clicks, password)
     │
     ▼
Publish ClickEvent to Kafka (async, <1ms)
     │
     ▼
HTTP 301/302/307/308 Redirect
```

- **`redirect_lookup`** — denormalized table for the hot path, avoids JOINs on the full `links` table.
- **`@Cacheable("redirects")`** — Redis cache, auto-evicted on link update/delete.
- Click event publishing is **non-blocking** (<1ms) — redirect is never delayed by analytics.

### 2. Kafka-Powered Analytics Pipeline

**Before**: 10,000 clicks/s → 10,000 INSERT statements → 10,000 DB round-trips.
**After**: 10,000 clicks/s → 20 batches × 500 rows → **20 DB round-trips (500× improvement)**.

- **At-least-once delivery**: Failed batch → Kafka offset not committed → messages redelivered.
- **Persistent**: Kafka stores on disk — no data loss on crash (unlike `ApplicationEventPublisher` in RAM).

### 3. Base62 Short Code Generation

```
PostgreSQL Sequence (100000, 100001, ...) → Base62 Encode → "q0U", "q0V", ...
```

Atomic DB sequence → guaranteed unique, no collision checks, 3-5 characters for millions of links.

### 4. Security

- **JWT** with Redis-backed blacklist validation (`JwtTimestampValidator` + `JwtBlacklistValidator`)
- **BCrypt** (cost=12), **SHA-256 IP anonymization**, RBAC with `@PreAuthorize`
- **Non-root Docker** (`USER spring`), secrets via environment variables

### 5. VNPay Payment Integration

- **IPN Webhook** (server-to-server) with HMAC-SHA512 signature verification
- Idempotent processing (duplicate IPN calls safely handled)
- Auto upgrade subscription on successful payment (FREE → PRO)

### 6. CI/CD Pipeline

```
Push to main → Build & Test → Docker Build → Trivy Scan → Push GHCR → Deploy Cloud Run
```

---

## 📡 API Reference

| Group | Endpoints | Auth |
|-------|-----------|------|
| **Auth** | `POST /api/v1/auth/register`, `/login`, `/refresh`, `/logout` | Public / JWT |
| **Links** | `POST/GET/PUT/DELETE /api/v1/links` | JWT |
| **Redirect** | `GET /r/{shortCode}` | Public |
| **Analytics** | `GET /api/v1/analytics/links/{id}/stats` | JWT |
| **Billing** | `POST /api/v1/billing/create-payment`, `GET /subscription` | JWT |
| **VNPay Webhook** | `GET /api/v1/billing/vnpay-ipn`, `/vnpay-return` | Public |
| **Admin** | `/api/v1/admin/links`, `/admin/analytics`, `/admin/billing` | Admin |

> 📄 Full interactive documentation at `http://localhost:8080/swagger-ui/index.html`

---

## 🗄 Database Design

Each module owns its own PostgreSQL schema — **no cross-schema foreign keys**. Inter-module references use `UUID` validated at application layer.

| Schema | Key Tables | Notes |
|--------|-----------|-------|
| `iam` | `users`, `roles`, `permissions`, `user_roles`, `refresh_tokens`, `token_blacklist`, `api_keys` | View: `user_effective_permissions` |
| `link` | `links`, `domains`, `redirect_lookup`, `tags`, `link_rules` | `redirect_lookup` = denormalized hot path |
| `analytics` | `click_events` *(partitioned)*, `click_agg_daily`, `click_agg_minute`, `link_counters` | BRIN index on `occurred_at` |
| `platform` | `outbox_events`, `audit_logs`, `idempotency_keys`, `blocked_domains`, `system_configs` | Infrastructure |
| `billing` | `subscriptions`, `payment_transactions` | VNPay integration |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+ (or use included `mvnw`)

### Setup

```bash
# 1. Clone
git clone https://github.com/ThanhND05/url_shortener_be.git
cd url_shortener_be

# 2. Start infrastructure (PostgreSQL, Redis, Kafka)
docker-compose up -d

# 3. Run application (Flyway auto-creates schema + seed data)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Verify
curl http://localhost:8080/actuator/health
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST` | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker(s) |
| `JWT_SECRET` | HMAC-SHA256 signing key |
| `FRONTEND_URL` | Frontend base URL |
| `ALLOWED_ORIGINS` | CORS allowed origins |
| `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL` | VNPay gateway config |

---

## ☁ Deployment

### Docker (Multi-stage)

```dockerfile
# Stage 1: Build (JDK 21 Alpine)
FROM eclipse-temurin:21-jdk-alpine AS build
RUN ./mvnw clean package -B -DskipTests

# Stage 2: Runtime (JRE 21 Alpine, non-root)
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### CI/CD (GitHub Actions)

| Stage | Details |
|-------|---------|
| **Build & Test** | JDK 21, `./mvnw verify` with Testcontainers |
| **Docker Build** | Multi-stage build with BuildKit layer caching |
| **Trivy Scan** | Container vulnerability scanning (CRITICAL) |
| **Push to GHCR** | Tags: `sha-<commit>`, `latest`, semver |
| **Deploy** | `gcloud run deploy` with VPC Direct Egress |

### GCP Production

```
Internet ──► Cloud Run (auto-scaling)
                │
                │  ┌── VPC ────────────────────────────┐
                ├──┼──► Cloud SQL       (PostgreSQL)    │
                ├──┼──► Memorystore     (Redis)         │
                └──┼──► Compute Engine  (Kafka VM)      │
                   └────────────────────────────────────┘
```

**VPC Direct Egress** enables Cloud Run to reach private VPC resources — required because Memorystore has no public IP.

---

## 🧪 Testing

| Layer | Tools | Coverage |
|-------|-------|----------|
| **Unit** | JUnit 5, Mockito | Service logic, Base62 encoding, billing |
| **Integration** | Testcontainers, MockMvc | Link creation → redirect → analytics (full flow) |

```bash
./mvnw verify    # Run all tests (requires Docker for Testcontainers)
```

---

## 🔮 Future Improvements

- [ ] Rate Limiting (Bucket4j)
- [ ] GeoIP Lookup (MaxMind GeoLite2)
- [ ] Observability (Prometheus + Grafana)
- [ ] Microservices Extraction
- [ ] Real-time WebSocket Dashboard

---

> This is a personal portfolio project built to demonstrate proficiency in modern backend architecture, cloud-native deployment, and production-grade engineering practices.
