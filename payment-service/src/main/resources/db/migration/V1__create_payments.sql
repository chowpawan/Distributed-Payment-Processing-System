CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE payments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      VARCHAR(255) NOT NULL,
    amount           NUMERIC(19, 4) NOT NULL,
    currency         CHAR(3) NOT NULL,
    status           VARCHAR(50) NOT NULL,
    stripe_charge_id VARCHAR(255),
    metadata         JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_status_created ON payments(status, created_at);
