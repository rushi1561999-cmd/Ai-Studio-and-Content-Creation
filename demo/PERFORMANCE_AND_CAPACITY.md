# AI Studio production performance and capacity

## What is implemented

The project now separates interactive API work from expensive provider work:

- Prompt Assistant requests are debounced in the browser by 750 ms, require 20 characters, reject prompts above 4,000 characters, cancel stale HTTP calls, and cache identical results for five minutes.
- The API enforces 10 suggestion requests per user per minute and subscription-aware daily quotas (free 20, paid 200, high-credit 500 by default). Redis provides shared counters in the Docker/production profile; a single-instance in-memory fallback keeps local development available.
- Prompt suggestions time out after five seconds and return deterministic rule-based suggestions if Gemini is missing, slow, rate-limited, or unavailable.
- Job state is pushed over an authenticated Server-Sent Events stream. The former two/four-second polling loop has been removed.
- RabbitMQ uses independent text, image, video, and prompt-suggestion queues. A transactional outbox prevents a committed job from being lost between MySQL and RabbitMQ.
- Local fallback workers use separate prompt, text, media, and email executors. Slow media work cannot consume every text/email worker.
- Workspace history, generated content, and saved prompts are paginated and their high-use columns are indexed.
- Gemini and Replicate share explicit connection/read timeouts. Gemini has temporary-error retry behavior, a small circuit breaker, request correlation IDs, configurable model URL, and structured JSON mode.
- Caffeine is the default one-instance cache. The Docker profile uses Redis for cross-instance caching.
- Actuator and Prometheus metrics are enabled. The optional monitoring profile provisions Prometheus and Grafana.
- React routes are split into lazy chunks, reference API calls use TanStack Query, static assets receive immutable Nginx caching, and JSON/JS/CSS/SSE responses are compressed where appropriate.

## Capacity is a measured result, not a fixed property

Three numbers must be kept separate:

| Measure | Meaning |
|---|---|
| Logged-in users | Mostly idle JWT holders; normally inexpensive |
| Active users | Users continuously loading pages and calling APIs |
| Concurrent AI jobs | Provider-backed text/image/video work actively executing |

The original framework defaults were approximately 200 Tomcat request threads, 10 Hikari database connections, and 8 common async workers. They did **not** mean “200 supported users.” The new configuration makes pool sizes explicit and isolates expensive work, but real capacity still depends on CPU, memory, database tier, prompt size, provider quotas, media duration, cache hit rate, and traffic shape.

## Planning targets

Use these as test goals, not a public guarantee:

| Component | Initial production design |
|---|---|
| API | 2 Spring Boot replicas, 4 vCPU / 8 GB each |
| Database | Managed MySQL, 20 connections per API replica initially |
| Cache | Redis |
| Queue | RabbitMQ with durable queues and prefetch 1 |
| Text workers | 8–16 per worker deployment |
| Image workers | 2–4 per worker deployment |
| Video workers | 1–2 per worker deployment |
| Frontend | Nginx behind a CDN/load balancer |
| Initial active-user target | 500 |
| Ordinary API throughput target | 50–100 requests/second |
| Ordinary API target | p95 < 500 ms |
| Error target | < 1% |
| AI job acceptance | < 300 ms |
| AI completion | Provider/content-type dependent |

The previous single-machine planning estimate was approximately 50–100 actively browsing users and about eight simultaneous default-async AI jobs. The new architecture is designed to remove those specific bottlenecks, but no higher number should be claimed until the supplied k6 tests pass on the intended infrastructure.

## Test and scale procedure

1. Use production-like data volume and hardware.
2. Run `load-tests/capacity.js` at 25 → 50 → 100 → 250 → 500 users.
3. Test login, dashboard, workspace/prompts, Prompt Assistant, job submission, completion delivery, marketplace/assets, and admin paths independently.
4. Run a one-hour endurance test, then a sudden traffic spike.
5. Watch `http.server.requests`, JVM heap/GC, Tomcat busy threads, Hikari pending/acquired connections, RabbitMQ queue depth/age, Redis latency/hit rate, MySQL slow queries, and provider 429/5xx responses.
6. Stop increasing load when p95 exceeds 500 ms, errors exceed 1%, CPU stays above 75–80%, memory grows continuously, the database pool queues, RabbitMQ age grows, or provider throttling begins.
7. Add API/worker replicas, then repeat at 750 and 1,000 users. Do not solve provider latency by only increasing Tomcat threads.

Start the local production topology with:

```bash
cp .env.example .env
docker compose --profile monitoring up --build
```

The application is then available on port 3000, RabbitMQ management on 15672, Prometheus on 9090, and Grafana on 3001.

## Operational safety

Previously tracked local/provider credentials must be rotated. Removing a secret from the current files does not remove it from Git history. Rotate every exposed credential first, then use an approved history-rewrite process if the repository has been shared.

For multi-replica production, keep `APP_CACHE_PROVIDER=redis` and `PROMPT_REDIS_RATE_LIMIT_ENABLED=true`. Configure queue/database/Redis alerts, automated backups, TLS, secret-manager injection, and provider spending/rate limits before public traffic.

## Viva description

> The Prompt Assistant is a guarded hybrid AI feature: the browser waits for a complete pause, the backend validates workspace access and subscription limits, Redis prevents abuse and caches duplicate requests, Gemini produces structured optimized variants within a five-second deadline, and a deterministic quality engine keeps the feature available when the provider fails. Content generation is accepted quickly, persisted through a transactional outbox, processed by workload-specific RabbitMQ workers, and reported to the authenticated user through server-sent events instead of high-frequency polling.
