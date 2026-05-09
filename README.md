# Distributed Payment Processing System

A production-style payment backend built with 4 Spring Boot microservices. Demonstrates saga orchestration, idempotent API design, event-driven architecture, and reconciliation against an external payment provider (Stripe).

## Architecture

```
Client
  │  HTTP
  ▼
payment-service (8080)
  │  POST /v1/payments  — Idempotency-Key header required
  │  GET  /v1/payments/{id}
  │  GET  /v1/payments?customerId=X
  │  POST /v1/payments/{id}/refund
  │
  │  Idempotency: Redis SETNX (24h TTL) → Postgres fallback
  │
  │  publishes PaymentInitiated → Kafka
  ▼
Kafka  (payment-events topic)
  │
  ├──► saga-orchestrator (8081)
  │      State machine: ChargeStep → RecordStep → NotifyStep
  │      Persists saga_state after each step (crash recovery)
  │      ChargeStep: searches Stripe before creating (prevents double charge)
  │
  └──► notification-service (8083)
         Mock email (log) + HMAC-SHA256 signed webhook delivery
         5 retries with exponential backoff

recon-worker (8082)
  @Scheduled every 5 min
  Finds PENDING payments older than 2 min
  Reconciles against Stripe, writes recon_audit_log
```

## Services

| Service | Port | Responsibility |
|---|---|---|
| payment-service | 8080 | REST API, idempotency, Kafka producer |
| saga-orchestrator | 8081 | Saga state machine, Stripe charge |
| recon-worker | 8082 | Reconciliation against Stripe |
| notification-service | 8083 | Webhooks + mock email |

## Tech Stack

- **Java 17** · Spring Boot 3.2.5 · Maven multi-module
- **PostgreSQL 15** · Flyway migrations (per-service history tables)
- **Redis 7** · idempotency key cache (SETNX, 24h TTL)
- **Kafka** (Confluent 7.5) · manual ack, at-least-once delivery
- **Stripe Java SDK** · charge search before create
- **Micrometer + Prometheus** · custom payment metrics

---

## Local Setup

### Prerequisites

- Java 17
- Maven 3.8+
- Docker Desktop (for Postgres, Redis, Kafka)

### 1. Clone and configure environment

```bash
git clone https://github.com/chowpawan/Distributed-Payment-Processing-System.git
cd Distributed-Payment-Processing-System

cp .env.example .env
# Edit .env and set your Stripe restricted test key:
# STRIPE_API_KEY=rk_test_...
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

Wait for all containers to be healthy (~30s):

```bash
docker-compose ps
# All four should show: (healthy)
```

### 3. Load environment variables

```bash
export $(grep -v '^#' .env | xargs)
```

### 4. Build all modules

```bash
mvn clean install -DskipTests
```

### 5. Start services (4 terminals)

```bash
# Terminal 1 — payment-service
mvn -pl payment-service spring-boot:run

# Terminal 2 — saga-orchestrator
mvn -pl saga-orchestrator spring-boot:run

# Terminal 3 — recon-worker
mvn -pl recon-worker spring-boot:run

# Terminal 4 — notification-service
mvn -pl notification-service spring-boot:run
```

### 6. Verify all services are up

```bash
curl http://localhost:8080/actuator/health   # payment-service
curl http://localhost:8081/actuator/health   # saga-orchestrator
curl http://localhost:8082/actuator/health   # recon-worker
curl http://localhost:8083/actuator/health   # notification-service
```

All should return `{"status":"UP"}`.

---

## Try It

### Create a payment

```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-001" \
  -H "X-Customer-Id: cust_001" \
  -d '{"amount": 9999, "currency": "usd", "metadata": {"order_id": "ord_001"}}'
```

Response `201`:
```json
{
  "id": "...",
  "status": "PENDING",
  "stripeChargeId": null
}
```

Within ~1 second, saga-orchestrator processes the Kafka event and hits Stripe. Poll again:

```bash
curl http://localhost:8080/v1/payments/{id}
# status: "COMPLETED", stripeChargeId: "ch_..."
```

### Test idempotency

```bash
# Send the same Idempotency-Key twice — same response, no second Stripe charge
curl -X POST http://localhost:8080/v1/payments \
  -H "Idempotency-Key: test-001" \
  -H "X-Customer-Id: cust_001" \
  -H "Content-Type: application/json" \
  -d '{"amount": 9999, "currency": "usd"}'
```

### List payments

```bash
curl "http://localhost:8080/v1/payments?customerId=cust_001&page=0&size=20"
```

### Refund (requires COMPLETED payment)

```bash
curl -X POST http://localhost:8080/v1/payments/{id}/refund \
  -H "Idempotency-Key: refund-001" \
  -H "X-Customer-Id: cust_001" \
  -H "Content-Type: application/json" \
  -d '{"amount": 4999, "reason": "customer_request"}'
```

---

## Running Tests

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn test -Dgroups=integration
```

---

## Postman Collection

Import [`postman/payment-system.postman_collection.json`](postman/payment-system.postman_collection.json).

Set collection variable `baseUrl = http://localhost:8080`. The collection includes:
- Create payment (auto-saves `paymentId`)
- Duplicate create (idempotency verification)
- Get by ID
- Paginated list
- Refund
- Health checks for all 4 services

---

## Database Schema

Single PostgreSQL database (`payments`) shared across all services. Each service uses a distinct Flyway history table to avoid migration conflicts.

| Table | Owner |
|---|---|
| `payments` | payment-service |
| `payment_attempts` | payment-service |
| `idempotency_keys` | payment-service |
| `audit_log` | payment-service |
| `saga_state` | saga-orchestrator |
| `recon_audit_log` | recon-worker |
| `webhook_subscriptions` | notification-service |

---

## Docs

- [Architecture](docs/architecture.md)
- [API Reference](docs/api.md)
- [Failure Modes](docs/failure-modes.md)
- [Design Decisions](docs/design-decisions.md)
