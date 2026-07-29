# AI Studio load tests

Install [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/), run the application with production-like resources, and use accounts created only for performance testing.

## Browsing/API capacity

```bash
BASE_URL=http://localhost:8081/api \
TEST_EMAIL=load-test@example.com \
TEST_PASSWORD='replace-me' \
k6 run capacity.js
```

This test ramps 25 → 50 → 100 → 250 → 500 virtual users and fails when p95 latency exceeds 500 ms or errors reach 1%.

## Prompt Assistant

Create one account per concurrent virtual user when validating rate limits. Do not use real customer accounts.

```bash
export K6_USERS='[
  {"email":"load-1@example.com","password":"replace-me","workspaceId":"workspace-id-1"},
  {"email":"load-2@example.com","password":"replace-me","workspaceId":"workspace-id-2"}
]'
k6 run prompt-assistant.js
```

Run login, dashboard, workspace/prompt, prompt-assistant, generation acceptance, job completion, marketplace, asset, admin, one-hour endurance, and sudden-spike scenarios independently. Provider-backed generation tests can incur cost and should use a dedicated provider project with explicit spending limits.
