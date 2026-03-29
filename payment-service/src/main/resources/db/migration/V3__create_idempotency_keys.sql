CREATE TABLE idempotency_keys (
    key_hash        VARCHAR(64) PRIMARY KEY,
    customer_id     VARCHAR(255) NOT NULL,
    response_status INT NOT NULL,
    response_body   TEXT NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);
