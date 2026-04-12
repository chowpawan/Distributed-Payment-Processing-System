CREATE TABLE recon_audit_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id    UUID NOT NULL,
    local_status  VARCHAR(50) NOT NULL,
    stripe_status VARCHAR(50),
    action_taken  VARCHAR(100) NOT NULL,
    notes         TEXT,
    reconciled_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recon_payment_id ON recon_audit_log(payment_id);
CREATE INDEX idx_recon_reconciled_at ON recon_audit_log(reconciled_at);
