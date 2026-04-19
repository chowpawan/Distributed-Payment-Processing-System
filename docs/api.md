# API Reference

Base URL: `http://localhost:8080`

All requests that create or modify resources require:
- `Idempotency-Key` header: 8–64 characters, base64 charset
- `X-Customer-Id` header: customer identifier

---

## Create Payment

`POST /v1/payments`

### Headers

| Header | Required | Description |
|---|---|---|
| `Idempotency-Key` | Yes | Unique key to prevent duplicate payments |
| `X-Customer-Id` | Yes | Customer identifier |
| `Content-Type` | Yes | `application/json` |

### Request Body

```json
{
  "amount": 9999,
  "currency": "usd",
  "metadata": {
    "order_id": "ord_001"
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `amount` | number | Yes | Amount (positive, up to 4 decimal places) |
| `currency` | string | Yes | 3-letter ISO currency code (e.g. `usd`) |
| `metadata` | object | No | Key-value pairs for your reference |

### Response `201 Created`

```json
{
  "id": "a1b2c3d4-...",
  "customerId": "cust_001",
  "amount": 9999,
  "currency": "usd",
  "status": "PENDING",
  "stripeChargeId": null,
  "metadata": { "order_id": "ord_001" },
  "createdAt": "2024-04-01T10:00:00Z",
  "updatedAt": "2024-04-01T10:00:00Z"
}
```

**Idempotency behavior:** Sending the same `Idempotency-Key` again returns the original `201` response from cache. No new payment is created.

### Status values

| Status | Meaning |
|---|---|
| `PENDING` | Payment created, saga not yet started or in progress |
| `COMPLETED` | Charge succeeded, recorded in Stripe |
| `FAILED` | Charge failed or saga compensation ran |
| `REFUNDED` | Full refund processed |
| `PARTIALLY_REFUNDED` | Partial refund processed |

### Error Responses

| Status | Condition |
|---|---|
| `400` | Missing required fields or validation failure |
| `400` | Missing `Idempotency-Key` header |

---

## Get Payment

`GET /v1/payments/{id}`

### Response `200 OK`

Same shape as Create Payment response.

### Error Responses

| Status | Condition |
|---|---|
| `404` | Payment not found |

---

## List Payments

`GET /v1/payments?customerId=cust_001&page=0&size=20`

### Query Parameters

| Parameter | Required | Default | Description |
|---|---|---|---|
| `customerId` | Yes | — | Filter by customer |
| `page` | No | `0` | Zero-based page number |
| `size` | No | `20` | Results per page |

### Response `200 OK`

```json
{
  "payments": [ { ... }, { ... } ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3
}
```

---

## Refund Payment

`POST /v1/payments/{id}/refund`

### Headers

Same as Create Payment (`Idempotency-Key`, `X-Customer-Id`).

### Request Body

```json
{
  "amount": 4999,
  "reason": "customer_request"
}
```

### Response `200 OK`

Payment response with `status: REFUNDED`.

### Error Responses

| Status | Condition |
|---|---|
| `404` | Payment not found |
| `422` | Payment is not in `COMPLETED` status |

---

## Health Check

`GET /actuator/health`

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

## Metrics

`GET /actuator/prometheus` — Prometheus-format metrics including:
- `payments.created.total`
- `payments.refunded.total`
- `payments.idempotency.hits.total`
