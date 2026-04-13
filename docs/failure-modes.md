# Failure Modes

How the system behaves when things go wrong.

| Failure | What Happens | How This System Handles It |
|---|---|---|
| Stripe API timeout (30s) | `ChargeStep` throws `SagaStepException` | Compensation runs: payment → `FAILED`, `PaymentFailed` event published. Client must retry with same idempotency key. |
| Service crashes mid-saga | Saga state lost in memory | `saga_state` table has `current_step` persisted after each step. On restart, a Saga Recovery job (not yet implemented — see known gaps) can resume from `current_step`. |
| Kafka consumer dies before offset commit | `PaymentInitiated` redelivered | `SagaOrchestrator.execute()` checks `existsByPaymentId()` at entry. Duplicate events are silently skipped. `ChargeStep` also queries Stripe by `metadata.payment_id` before creating a new charge. |
| Network split between this system and Stripe | Local says `PENDING`, Stripe processed | `recon-worker` polls Stripe every 5 minutes. Worst-case reconciliation latency: ~7 minutes (2-minute grace period + 5-minute poll). |
| Duplicate idempotency key from client retry | Second request arrives at `payment-service` | Redis SETNX returns `false` → response from cache returned immediately. No second payment created. |
| Webhook delivery to customer fails | `POST` to customer URL returns 5xx or times out | `WebhookDispatcher` retries up to 5 times with exponential backoff (1s, 2s, 4s, 8s). After 5 failures, logs permanently failed (production: write to `failed_deliveries` table). |
| Redis unavailable | Idempotency check fails at Layer 1 | `IdempotencyServiceImpl` catches Redis exception, falls back to PostgreSQL `idempotency_keys` table. |
| DB connection pool exhausted (HikariCP max=20) | Requests fail with 503 | HikariCP blocks for `connection-timeout` (30s), then throws. No graceful degradation in this project — acknowledging this limitation. |
| Kafka broker down when `payment-service` publishes | `PaymentInitiated` event not delivered | Payment is saved in DB as `PENDING`. Kafka producer retries (3 attempts, `acks=all`). If all fail, payment stays `PENDING`. `recon-worker` marks it `FAILED` after 30 minutes with no Stripe record. |
| Stripe charge created but `RecordStep` fails | Stripe charged, local still `PENDING` | `recon-worker` finds the Stripe charge via metadata search and updates local status to `COMPLETED`. |

## Known Gaps (Production Improvements)

1. **Saga Recovery Job** — On `saga-orchestrator` restart, `RUNNING` sagas are not automatically resumed. A startup job querying `saga_state WHERE status='RUNNING'` would re-enqueue these.

2. **Outbox Pattern** — `payment-service` publishes Kafka events inside the same thread as the DB save but outside the transaction. If Kafka is permanently unavailable after a payment is saved, the event is never delivered. A transactional outbox table would solve this.

3. **Webhook Async Retry Queue** — `WebhookDispatcher` blocks the Kafka consumer thread during retries. Production: write to a `webhook_deliveries` table and retry asynchronously via `@Scheduled`.

4. **DLQ (Dead Letter Queue)** — Failed Kafka messages are currently just left unacked. In production, after N retries, route to a DLQ topic for manual inspection.
