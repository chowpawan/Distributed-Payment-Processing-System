# Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                            Client                                    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ HTTP
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│  payment-service (8080)                                              │
│  - POST /v1/payments  (Idempotency-Key header required)             │
│  - GET  /v1/payments/{id}                                           │
│  - GET  /v1/payments?customerId=X                                   │
│  - POST /v1/payments/{id}/refund                                    │
│                                                                      │
│  Idempotency: Redis SETNX (24h TTL) → Postgres fallback             │
└────────────────────────┬────────────────────────────────────────────┘
                         │ publishes PaymentInitiated
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Kafka  (payment-events topic)                                       │
└────────────────────┬──────────────────────┬─────────────────────────┘
                     │                      │
          PaymentInitiated        PaymentCompleted / PaymentFailed
                     │                      │
                     ▼                      ▼
┌──────────────────────────┐  ┌────────────────────────────────────┐
│  saga-orchestrator (8081)│  │  notification-service (8083)       │
│                          │  │  - mock email (log)                │
│  State machine:          │  │  - HMAC-signed webhook delivery    │
│  ChargeStep → Stripe     │  │    (5 retries, exponential backoff)│
│  RecordStep → Update DB  │  └────────────────────────────────────┘
│  NotifyStep → Kafka      │
│                          │
│  saga_state table:       │
│  persists step progress  │
│  for crash recovery      │
└──────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  recon-worker (8082)                                                 │
│  @Scheduled every 5 minutes                                          │
│  Queries: payments WHERE status=PENDING AND created > 2 min ago     │
│  For each: GET Stripe /v1/charges?metadata[payment_id]=...          │
│  Reconciles discrepancies, writes to recon_audit_log                │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  PostgreSQL  (shared, port 5432)   │  Redis  (port 6379)            │
│  - payments                        │  - idempotency keys (24h TTL)  │
│  - payment_attempts                │                                 │
│  - idempotency_keys (DB fallback)  │                                 │
│  - saga_state                      │                                 │
│  - recon_audit_log                 │                                 │
│  - webhook_subscriptions           │                                 │
└─────────────────────────────────────────────────────────────────────┘
```

## Payment Flow (Happy Path)

```
Client → POST /v1/payments (Idempotency-Key: abc123)
         │
         ├─ Redis check: key not found
         ├─ Save Payment{status=PENDING} to DB
         ├─ Publish PaymentInitiated → Kafka
         ├─ Cache response in Redis (24h)
         └─ Return 201 {id, status: PENDING}

Kafka → saga-orchestrator consumes PaymentInitiated
         │
         ├─ Create saga_state{status=RUNNING, step=CHARGE}
         ├─ ChargeStep: search Stripe by metadata.payment_id
         │              → no match → create Stripe charge
         │              → save saga_state{step=CHARGE done}
         ├─ RecordStep: UPDATE payments SET status=COMPLETED, stripe_charge_id=...
         │              → save saga_state{step=RECORD done}
         ├─ NotifyStep: publish PaymentCompleted → Kafka
         │              → save saga_state{status=COMPLETED}
         └─ Done

Kafka → notification-service consumes PaymentCompleted
         ├─ EmailNotificationHandler: logs mock email
         └─ WebhookNotificationHandler: POST to customer webhook URL
                                        with X-Webhook-Signature header
```

## Kafka Topics

| Topic | Producers | Consumers |
|---|---|---|
| `payment-events` | payment-service, saga-orchestrator | saga-orchestrator, notification-service |

All event types share one topic, discriminated by `eventType` field in JSON.

## Service Ports

| Service | Port |
|---|---|
| payment-service | 8080 |
| saga-orchestrator | 8081 |
| recon-worker | 8082 |
| notification-service | 8083 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |
| Zookeeper | 2181 |

## Database Schema (single PostgreSQL database: `payments`)

Each service uses a distinct Flyway history table to avoid migration conflicts:
- `payment-service` → `flyway_payment_schema_history`
- `saga-orchestrator` → `flyway_saga_schema_history`
- `recon-worker` → `flyway_recon_schema_history`
- `notification-service` → `flyway_notification_schema_history`
