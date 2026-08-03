# Production Launch Checklist

Use this checklist for the CDAC demonstration and again before any public launch. Provider credentials must be placed in `.env` or the deployment secret manager, never committed.

## 1. Secrets and accounts

- [ ] Generate a unique `JWT_SECRET` with at least 32 random characters/bytes.
- [ ] Replace MySQL root/app passwords and RabbitMQ credentials.
- [ ] Add a valid `GEMINI_API_KEY` and/or `REPLICATE_API_TOKEN`.
- [ ] Configure one complete payment gateway; incomplete gateways remain disabled.
- [ ] If password reset/login mail is demonstrated, configure SMTP and set `APP_EMAIL_ENABLED=true`.
- [ ] If an admin must be bootstrapped, set `APP_ADMIN_SEED_ENABLED=true`, email, and a 10+ character password for the first startup. Set it back to `false` afterward.
- [ ] Confirm `.env` and `application-local.properties` are ignored by Git.
- [ ] Rotate any secret that has ever been committed or shared.

## 2. Provider webhooks

The backend must be reachable through public HTTPS before live callbacks can work.

### Razorpay

- [ ] Register `https://YOUR_API_HOST/api/razorpay/webhook`.
- [ ] Subscribe to `payment.captured`, `payment.failed`, and `order.paid`.
- [ ] Store the dashboard webhook secret as `RAZORPAY_WEBHOOK_SECRET` (separate from the API secret).
- [ ] First use Razorpay test mode, complete a checkout, confirm one `COMPLETED` payment and exactly one wallet credit transaction.
- [ ] Replay the same webhook and confirm the wallet balance does not change.

### Stripe

- [ ] Register `https://YOUR_API_HOST/api/stripe/webhook`.
- [ ] Subscribe to `checkout.session.completed`, `checkout.session.async_payment_succeeded`, and `checkout.session.async_payment_failed`.
- [ ] Store the endpoint signing secret as `STRIPE_WEBHOOK_SECRET`.
- [ ] First use Stripe test mode, complete a Checkout session, and confirm idempotent wallet/plan fulfillment.

Redirect success pages do not credit wallets. Only server-verified provider events fulfill a Stripe payment; Razorpay additionally verifies the signed browser result and checks the order/payment through its API.

## 3. Domain and deployment

- [ ] Put the frontend and API behind TLS; redirect HTTP to HTTPS at the ingress/load balancer.
- [ ] Set `APP_FRONTEND_URL` to the deployed frontend URL.
- [ ] Set `CORS_ALLOWED_ORIGINS` to only trusted deployed origins.
- [ ] Do not expose MySQL, Redis, RabbitMQ, Prometheus, or Grafana ports publicly.
- [ ] Restrict `/actuator` and monitoring tools with network policy or authentication.
- [ ] Persist and back up `mysql_data`, `asset_data`, RabbitMQ, and Redis volumes.
- [ ] Configure log aggregation, uptime checks, disk alerts, and database backup restore tests.
- [ ] For more than one backend replica, move assets to object storage before scaling.

## 4. Reproducible verification

```bash
cd demo/frontend
npm ci
npm run lint
npm run build

cd ../demo
./mvnw test

cd ..
docker compose --env-file .env config
docker compose --env-file .env up --build
```

- [ ] Readiness returns `UP` at `/actuator/health/readiness`.
- [ ] Register, login, logout, forgot password, and reset password work.
- [ ] A user cannot access another workspace by replacing an ID in a request.
- [ ] Upload, download, new version, and delete work for a real file.
- [ ] Text/image/video jobs use configured providers; a provider error fails and refunds the job.
- [ ] Insufficient credits prevents generation.
- [ ] Credit pack payment updates payment, wallet, transaction history, and audit log once.
- [ ] Paid plan stays pending until payment and becomes active only after verification.
- [ ] Failed/cancelled payment does not credit the wallet.
- [ ] Admin-only routes reject a normal user.
- [ ] Admin wallet adjustment requires a reason and appears in the ledger/audit history.
- [ ] No keys, passwords, local data, `node_modules`, build output, or IDE metadata are in the submission archive.

## 5. CDAC demonstration sequence

1. Show architecture and health endpoint.
2. Register and create the default workspace.
3. Generate content and show credit debit plus job history.
4. Upload/download an asset and create a new version.
5. Complete a provider sandbox payment and show the verified payment, wallet ledger, and duplicate protection.
6. Buy a paid plan and show that it activates only after payment.
7. Show admin metrics, audit history, payment history, and controlled manual adjustment.
8. Stop one provider or use an invalid configuration to demonstrate visible failure and credit refund.

## 6. Honest scope statement

This repository is production-oriented application code, not a claim that external accounts or infrastructure are already live. Automatic recurring billing, tax invoices/GST calculation, refunds/disputes, object-store malware scanning, formal penetration testing, and compliance certification require separate product and operational work before commercial launch.
