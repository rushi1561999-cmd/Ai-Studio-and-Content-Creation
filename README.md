# AI Studio and Content Creation

AI Studio is a multi-tenant content platform built with React, Spring Boot, MySQL, Redis, and RabbitMQ. It connects to real AI and payment providers, maintains an auditable credit wallet, supports paid 30-day plans, stores user assets with version history, and includes a moderated prompt marketplace and admin console.

## Implemented production flows

- JWT authentication, workspace isolation, roles, password reset email, and admin authorization
- Gemini text generation and Replicate image/video generation without synthetic fallback output
- Asynchronous jobs with RabbitMQ or bounded in-process executors, progress events, failure refunds, and audit logs
- Razorpay and Stripe hosted checkout with signature-verified webhooks
- Idempotent payment fulfillment, server-owned prices, wallet locking, and immutable credit history
- Paid plan activation only after verified payment; no direct activation or development top-up endpoint
- Real multipart asset upload, authenticated download, folders, versions, deletion, and persistent Docker storage
- Prometheus metrics, health probes, Redis caching/rate limits, Docker Compose, and optional Grafana monitoring

## Quick start

Requirements: Docker Desktop/Engine with Compose, 8 GB RAM recommended, and internet access for image/provider downloads.

```bash
cd demo
cp .env.example .env
```

Edit `.env`. At minimum, replace all database, RabbitMQ, and JWT placeholder values. Add Gemini and/or Replicate credentials for generation, and one complete payment gateway configuration for checkout.

```bash
docker compose config
docker compose up --build
```

Open `http://localhost:3000`. The API readiness probe is at `http://localhost:8081/actuator/health/readiness`.

No provider is silently simulated. If credentials are absent, that provider is shown as unavailable and paid actions stay disabled.

## Repository layout

| Path | Purpose |
|---|---|
| `demo/frontend` | React 19 + Vite web application |
| `demo/demo` | Java 21 + Spring Boot API |
| `demo/docker-compose.yml` | MySQL, Redis, RabbitMQ, API, UI, and monitoring stack |
| `demo/monitoring` | Prometheus and Grafana provisioning |
| `demo/load-tests` | k6 capacity scenarios |
| `PROJECT_DOCUMENTATION.md` | Architecture, features, data flows, and security notes |
| `PRODUCTION_LAUNCH_CHECKLIST.md` | Credential, webhook, deployment, and acceptance checklist |
| `IMPLEMENTATION_REPORT.md` | Dummy-mode removal and verification report |

## Development commands

Backend:

```bash
cd demo/demo
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
./mvnw test
./mvnw spring-boot:run
```

Frontend:

```bash
cd demo/frontend
npm ci
npm run lint
npm run build
npm run dev
```

The frontend development proxy sends `/api` to `http://localhost:8081`.

## Important launch boundary

The code contains real provider integrations, but a deployment is not live until you supply credentials in the deployment secret manager, publish the HTTPS endpoints, register both webhook URLs, test provider callbacks, configure backups, and complete the checklist. Never put API keys or passwords in source control or a support message.

See [PRODUCTION_LAUNCH_CHECKLIST.md](PRODUCTION_LAUNCH_CHECKLIST.md) for the exact steps.
