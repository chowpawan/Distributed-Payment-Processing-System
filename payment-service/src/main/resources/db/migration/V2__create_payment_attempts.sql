CREATE TABLE payment_attempts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id     UUID NOT NULL REFERENCES payments(id),
    attempt_number INT NOT NULL,
    status         VARCHAR(50) NOT NULL,
    error_message  TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attempts_payment_id ON payment_attempts(payment_id);
