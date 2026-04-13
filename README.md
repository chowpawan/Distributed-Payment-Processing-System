# Distributed Payment Processing System

A 4-service Spring Boot microservices system for processing payments via Stripe sandbox.

## Architecture

```
Client
  │
  ▼
payment-service (8080)          ← REST API, idempotency via Redis
  │  publishes PaymentInitiated
  ▼
Kafka (payment-events topic)
  │
  ├──► saga-orchestrator (8081) ← state machine: Stripe charge → record → notify
  │                               persists saga_state for crash recovery
  │
  └──► notification-service (8083) ← HMAC-signed webhooks + mock email
                                      listens to PaymentCompleted / PaymentFailed

recon-worker (8082)             ← @Scheduled every 5 min, reconciles PENDING vs Stripe
```

## Services

| Service | Port | Responsibility |
|---|---|---|
| payment-service | 8080 | REST API, idempotency, Kafka producer |
| saga-orchestrator | 8081 | Saga state machine, Stripe charge |
| recon-worker | 8082 | Reconciliation against Stripe |
| notification-service | 8083 | Webhooks + mock email |

## Local Setup

### Prerequisites
- Java 17
- Docker + Docker Compose
- Maven 3.8+

### 1. Start infrastructure
```bash
docker compose up -d
# Wait for all services to be healthy (~30s)
docker compose ps
```

### 2. Add your Stripe test key
```bash
export STRIPE_API_KEY=sk_test_your_key_here
```

Or create `payment-service/src/main/resources/application-local.yml`:
```yaml
stripe:
  api-key: sk_test_your_key_here
```

### 3. Build all modules
```bash
mvn clean install -DskipTests
```

### 4. Start services (separate terminals)
```bash
# Terminal 1
cd payment-service && mvn spring-boot:run

# Terminal 2
cd saga-orchestrator && mvn spring-boot:run

# Terminal 3
cd recon-worker && mvn spring-boot:run

# Terminal 4
cd notification-service && mvn spring-boot:run
```

### 5. Create a payment
```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-001" \
  -H "X-Customer-Id: cust_001" \
  -d '{"amount": 9999, "currency": "usd"}'
```

Retry with the same `Idempotency-Key` — you get the same response from Redis cache.

## Running Tests

```bash
# Unit tests (fast)
mvn test

# Integration tests (requires Docker)
mvn test -Dgroups=integration
```

## Key Design Decisions

See [docs/design-decisions.md](docs/design-decisions.md).

## API Reference

See [docs/api.md](docs/api.md).

## Failure Modes

See [docs/failure-modes.md](docs/failure-modes.md).

## Postman Collection

Import [postman/payment-system.postman_collection.json](postman/payment-system.postman_collection.json).
Set environment variable `baseUrl = http://localhost:8080`.
