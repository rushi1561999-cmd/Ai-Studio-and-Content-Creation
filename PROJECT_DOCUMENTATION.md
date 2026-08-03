# AI Studio — Project Documentation

## 1. Product overview

AI Studio lets authenticated users create workspaces, generate text/images/video using external AI providers, organize files, publish reusable prompts, receive notifications, and buy credits or paid 30-day plans. Platform administrators can manage users, models, plans, marketplace content, payments, wallet adjustments, and audit history.

The application follows a layered Spring architecture and a single-page React client. Workspace membership is the tenant boundary; services re-check workspace access before reading or changing tenant-owned records.

## 2. Runtime architecture

```text
Browser → Nginx/React → Spring Boot REST API → MySQL
                               ├────────────→ Redis
                               ├────────────→ RabbitMQ workers
                               ├────────────→ asset volume
                               ├────────────→ Gemini / Replicate
                               └────────────→ Stripe / Razorpay / SMTP
Payment providers ──signed webhook────────→ Spring Boot API
```

| Layer | Technology | Responsibility |
|---|---|---|
| Web client | React 19, Vite, Axios | Authenticated product and admin UI |
| API | Java 21, Spring Boot 4, Spring Security | REST endpoints, business rules, validation, authorization |
| Persistence | MySQL, JPA/Hibernate, Flyway | Tenant data, jobs, wallet ledger, payments, subscriptions, audit history |
| Work processing | RabbitMQ or bounded executors | Asynchronous AI generation |
| Fast state | Redis | Distributed rate limits, caching, and job-event fan-out |
| Observability | Actuator, Micrometer, Prometheus, Grafana | Readiness, metrics, dashboards |
| External services | Gemini, Replicate, Stripe, Razorpay, SMTP | Generation, hosted payments, transactional mail |

## 3. Core data and authorization model

- A `User` authenticates with a BCrypt password and receives a signed, expiring JWT.
- A `Workspace` owns the wallet, prompts, jobs, generated content, assets, subscriptions, and payments.
- `WorkspaceMember` links a user to a workspace with an owner/editor/viewer-style role.
- `Wallet` stores the current balance. Every change creates a `CreditTransaction` with type, reference, description, and balance-after.
- `Payment` is the gateway-neutral order record. Its `(provider, external_id)` pair is unique.
- `Subscription` begins as `PENDING`, becomes `ACTIVE` only through verified fulfillment, and has a 30-day validity date. It is not presented as automatic recurring billing.
- `AuditLog` records administrative, billing, generation, and storage activity.

Public authentication and signed provider webhook endpoints are the only unauthenticated API writes. `/api/admin/**` requires the platform `ADMIN` role; other `/api/**` routes require a JWT and domain services enforce workspace membership.

## 4. Payment, subscription, and wallet flow

1. The browser requests checkout for a server-defined credit pack or a pending paid plan.
2. The API validates workspace access and resolves amount, currency, and credits from server data. It never accepts a client-supplied price.
3. Stripe or Razorpay creates a hosted order/session; the API writes a `PENDING` payment.
4. The provider sends a signed webhook. Razorpay browser completion also performs server-side signature verification and provider lookup for a responsive UI.
5. `PaymentFulfillmentService` locks the payment row, confirms amount/currency, and returns immediately if already complete.
6. For a pack, the wallet is locked and credited. For a plan, the pending subscription is locked, activated for 30 days, and its plan credits are added.
7. The payment stores the provider payment identifier and becomes `COMPLETED`. Duplicate webhooks cannot add credits twice.

There is no public wallet top-up endpoint, direct plan activation endpoint, unverified QR payment path, or success-on-redirect fulfillment. Admin credit adjustments remain available as an intentional support operation and always create wallet and audit records.

## 5. AI generation flow

1. The API validates the prompt, selected content type/model, workspace membership, and available credits.
2. It debits the wallet and records the job.
3. The job runs through RabbitMQ when enabled, otherwise through a bounded executor intended for local development.
4. Gemini handles text; Replicate handles image/video. Missing credentials or provider failure produces a failed job—never fabricated content.
5. Success creates `GeneratedContent`; failure records a terminal state and refunds the generation credits.
6. Authenticated server-sent events update job progress; notifications are read through authenticated REST endpoints.

## 6. Asset storage

The asset API accepts multipart files up to the configured limit, removes unsafe path components, assigns random physical names, checks tenant ownership, and records versions. Downloads require authentication and workspace access. Docker persists files in the `asset_data` volume.

For a horizontally scaled public deployment, replace the local volume implementation with S3-compatible object storage and malware/type scanning. The current mounted-volume implementation is appropriate for a single-node CDAC deployment.

## 7. Security controls

- JWT secret must contain at least 32 bytes; startup fails if it is absent or weak.
- Passwords require at least 10 characters and are stored only as BCrypt hashes.
- Password reset tokens are random, hashed in storage, single-use, expire after one hour, and are sent through SMTP.
- Provider API keys and secrets come only from environment variables/untracked local properties.
- Razorpay uses HMAC-SHA256 signature checks plus provider-side order/payment retrieval.
- Stripe validates its webhook signature before fulfillment.
- Row locks and unique constraints protect wallet/payment idempotency.
- Global validation/error handling avoids exposing stack traces to clients.
- CORS origins are configurable; production should list only the deployed UI origin.
- Actuator exposure is restricted to health/info/metrics/Prometheus/cache endpoints.

## 8. Operational profiles

- Default/local: MySQL plus optional in-process workers and Caffeine cache.
- Docker: MySQL, Redis, RabbitMQ, mounted assets, readiness checks, Nginx frontend.
- Monitoring: `docker compose --profile monitoring up --build` additionally starts Prometheus and Grafana.

`spring.jpa.hibernate.ddl-auto=update` is retained for compatibility with the supplied educational schema while Flyway owns explicit performance and payment hardening migrations. A later multi-environment release should baseline the full schema in Flyway and switch Hibernate to `validate`.

## 9. Test strategy

- Backend unit tests cover prompt quality and idempotent payment fulfillment.
- Spring context test uses H2 in MySQL compatibility mode with external services disabled.
- Frontend lint and production build catch React, import, and bundling failures.
- k6 scripts exercise capacity-sensitive endpoints.
- Payment acceptance must include provider sandbox webhooks, duplicate delivery, failed payment, cancelled checkout, amount mismatch, and post-payment balance checks.

See the launch checklist for reproducible commands and live-provider acceptance criteria.
