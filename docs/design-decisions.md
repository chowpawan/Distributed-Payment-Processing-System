# Design Decisions

These are the 8 decisions I made during the build and the reasoning behind each.
Fill this in as you actually build — an interviewer will ask "why did you choose X?" and the answer needs to sound like yours.

---

## 1. PostgreSQL over MySQL

**Decision:**

**Why:**

**Trade-off:**

---

## 2. Redis over PostgreSQL for idempotency (primary layer)

**Decision:**

**Why:**

**Trade-off:**

---

## 3. Hand-rolled saga state machine over Spring Statemachine

**Decision:**

**Why:**

**Trade-off:**

---

## 4. Synchronous Stripe call (blocking in saga step)

**Decision:**

**Why:**

**Trade-off:**

---

## 5. Idempotency cache stores full response, not just presence flag

**Decision:**

**Why:**

**Trade-off:**

---

## 6. `saga_state` table persisted before each step

**Decision:**

**Why:**

**Trade-off:**

---

## 7. Reconciliation polls Stripe; does not rely solely on webhooks

**Decision:**

**Why:**

**Trade-off:**

---

## 8. Mock compensation, not real refunds

**Decision:** The compensation handler marks the payment as FAILED and emits a `PaymentFailed` event. It does not call the Stripe refund API.

**Why:** Real refunds require handling partial charges, KYC checks, accounting entries, and dispute management. These are out of scope for a learning project. The important thing is that the *structure* of compensation exists and is testable.

**Trade-off:** In a production system, you'd need a separate `refund-service` with its own saga to handle the refund lifecycle.
