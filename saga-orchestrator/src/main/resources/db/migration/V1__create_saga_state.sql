CREATE TABLE saga_state (
    saga_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id    UUID NOT NULL UNIQUE,
    current_step  VARCHAR(50) NOT NULL,
    status        VARCHAR(50) NOT NULL,
    last_error    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMPTZ
);

CREATE INDEX idx_saga_payment_id ON saga_state(payment_id);
CREATE INDEX idx_saga_status ON saga_state(status);
