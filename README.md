# Multi-tenant Notification Service

A Spring Boot service that lets multiple tenants send notifications across
Email, SMS, Push, and In-App channels using tenant-defined templates, with
per-tenant rate limiting, scheduled sends, retries with backoff, and a full
delivery audit trail.

> **Note:** This is a take-home assignment submission, scoped for demo
> purposes (see [Assumptions](#assumptions--scope-decisions)). It is **not**
> production-hardened.

---

## Tech Stack

| Concern              | Choice                                   |
|----------------------|------------------------------------------|
| Language             | Java 17                                   |
| Framework            | Spring Boot 3.2.x                         |
| Persistence          | Spring Data JPA + MySQL (H2 for tests)    |
| Auth                  | Spring Security + JWT (stateless)         |
| Rate limiting        | Bucket4j (in-memory token bucket)         |
| Scheduling/Dispatch  | `@Scheduled` poller + `ThreadPoolTaskExecutor` per channel |
| Build                | Maven                                     |
| Testing              | JUnit 5, Spring Boot Test, MockMvc, H2    |

---

## Entity-Relationship Diagram

```
┌────────────────────┐
│      Tenant         │
│─────────────────────│
│ id (PK)              │
│ name (unique)        │
│ status               │
│ global_rate_limit... │
└─────────┬────────────┘
          │ 1
          │
          │ N
┌─────────┴────────────┐        ┌──────────────────────┐
│        User           │        │   ChannelConfig       │
│────────────────────────│        │────────────────────────│
│ id (PK)                 │        │ id (PK)                 │
│ email (unique)          │        │ tenant_id (FK) ─────────┼─┐
│ password_hash           │        │ channel                 │ │
│ role (PLATFORM/TENANT)  │        │ enabled                 │ │
│ tenant_id (FK, nullable)│        │ sender_identifier       │ │
└──────────────────────────┘        └─────────────────────────┘ │
                                                                  │
┌────────────────────────┐        ┌──────────────────────────┐ │
│       Template           │        │   RateLimitConfig          │ │
│────────────────────────────│        │──────────────────────────────│ │
│ id (PK)                     │        │ id (PK)                       │ │
│ tenant_id (FK) ──────────────┼─┐      │ tenant_id (FK) ────────────────┼─┘
│ name                         │ │      │ channel (nullable = all)       │
│ channel                      │ │      │ capacity_per_minute            │
│ subject                      │ │      └──────────────────────────────────┘
│ body ({{variables}})         │ │
│ version                      │ │
└─────────────────────────────────┘ │
          │ 1                        │
          │                          │  (Tenant 1───N for all of the above)
          │ N                        │
┌─────────┴──────────────────────┐  │
│          Notification             │◄─┘
│──────────────────────────────────│
│ id (PK)                            │
│ tenant_id (FK)                     │
│ template_id (FK)                   │
│ channel                            │
│ recipient                          │
│ template_variables (JSON)          │
│ idempotency_key (unique w/ tenant) │
│ status (state machine)             │
│ scheduled_at                       │
│ attempt_count                      │
│ next_attempt_at                    │
│ rendered_content                   │
│ lock_version (optimistic lock)     │
└─────────────┬──────────────────────┘
              │ 1
              │
              │ N
┌─────────────┴──────────────────────┐
│        NotificationAttempt           │
│───────────────────────────────────────│
│ id (PK)                                 │
│ notification_id (FK)                    │
│ attempt_number                          │
│ status (SUCCESS/TRANSIENT/PERMANENT)    │
│ started_at                              │
│ completed_at                            │
│ error_message                           │
└─────────────────────────────────────────┘
```

### Notification status state machine

```
PENDING ──► PROCESSING ──► SENT (terminal)
                │
                ├──► RETRYING ──► PROCESSING  (loops until max retries)
                │                     │
                │                     └──► FAILED_PERMANENT (terminal)
                │
                └──► FAILED_PERMANENT (terminal, on non-retryable error)

PENDING ──► CANCELLED (terminal, scheduled notification cancelled before dispatch)
```

---

## Core Entities

| Entity | Purpose |
|---|---|
| `Tenant` | Top-level isolation boundary. Owns templates, channel configs, rate limits, users, notifications. |
| `User` | Platform admin (global) or tenant admin (scoped to one tenant). |
| `Template` | Tenant-owned, per-channel message template with `{{variable}}` placeholders. |
| `ChannelConfig` | Per-tenant, per-channel enable/disable + sender identifier (simulated). |
| `RateLimitConfig` | Per-tenant (optionally per-channel) requests-per-minute limit. Falls back to system default. |
| `Notification` | A single send request; the unit the dispatch engine processes. Tracks status, scheduling, retry bookkeeping, and idempotency. |
| `NotificationAttempt` | Immutable audit row per delivery attempt (success/transient/permanent failure), forming the retry audit trail. |

---

## Roles & Access Control

- **PLATFORM_ADMIN**: manage tenants, global rate limit overrides, view cross-tenant stats. Not bound to a tenant.
- **TENANT_ADMIN**: manage their own tenant's templates, channel configs, submit notifications, view delivery reports. Bound to exactly one tenant via `User.tenant`.

Enforced via Spring Security method-level `@PreAuthorize` checks plus a tenant-scoping check (a `TENANT_ADMIN` can only access resources belonging to their own `tenant_id`).

---

## Assumptions & Scope Decisions

- **Channel sending is simulated.** No real integration with email/SMS/push providers (Twilio, SendGrid, FCM, etc.). A mock sender introduces artificial latency and a configurable random failure rate to exercise the retry/backoff path.
- **Auth is simplified JWT**, no OAuth/SSO/MFA, per the assignment's out-of-scope list.
- **Rate limiting is in-memory (Bucket4j)**, single-instance. A production system would need a distributed store (e.g. Redis) — explicitly out of scope ("Distributed systems... out of scope").
- **Scheduling uses a polling `@Scheduled` task**, not a message queue/broker, again per the out-of-scope list (no microservices/distributed infra).
- **Duplicate prevention**: enforced via a unique constraint on `(tenant_id, idempotency_key)`. Clients are expected to pass an idempotency key for at-least-once-safe submission; without one, no duplicate check is performed (each call creates a new notification).
- **"Fairness" definition**: implemented as round-robin selection across tenants with pending work within each dispatch poll cycle, combined with per-tenant token-bucket rate limiting, so no single tenant can starve others of worker capacity.
- **Template versioning**: a simple integer `version` counter on `Template`, incremented on update. The exact rendered content sent is snapshotted onto `Notification.renderedContent` for audit purposes, independent of later template edits.
- **`ddl-auto: update`** is used for convenience in this demo; a production setup would use versioned migrations (Flyway/Liquibase).
- **Retry policy**: exponential backoff (`backoff-base-ms * backoff-multiplier^attempt`), capped at `max-retries` (default 3), after which the notification moves to `FAILED_PERMANENT`.

---

## Running Locally

### Prerequisites
- Java 17
- Maven 3.8+
- MySQL 8 running locally (or update `application.yml` datasource)

### Setup

```bash
# Create database (or let createDatabaseIfNotExist handle it)
mysql -u root -p -e "CREATE DATABASE notification_service;"

# Run
mvn spring-boot:run
```

Default config connects to `jdbc:mysql://localhost:3306/notification_service`
with `root`/`root`. Override via `DB_USERNAME` / `DB_PASSWORD` env vars.

### Running Tests

```bash
mvn test
```

Tests run against an in-memory H2 database (MySQL-compatible mode), no
external MySQL instance required.

---

## Project Status

This README will be expanded as features are implemented. Current state:
project scaffolding (Maven, Java 17, Spring Boot) and core JPA entities are
in place. See commit history for incremental progress.

---

## AI-Assisted Development

This project was developed with AI assistance (Claude). See `Claude.md` for
the workflow, prompts, and skills used during development.
