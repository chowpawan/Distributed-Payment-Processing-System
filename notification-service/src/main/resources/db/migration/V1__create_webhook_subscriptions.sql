CREATE TABLE webhook_subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL,
    url         VARCHAR(2048) NOT NULL,
    secret      VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE webhook_subscription_events (
    subscription_id UUID NOT NULL REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
    event_type      VARCHAR(100) NOT NULL,
    PRIMARY KEY (subscription_id, event_type)
);

CREATE INDEX idx_webhook_customer_id ON webhook_subscriptions(customer_id);
