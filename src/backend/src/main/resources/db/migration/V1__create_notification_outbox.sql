CREATE TABLE IF NOT EXISTS notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    template_type VARCHAR(30) NOT NULL,
    recipient VARCHAR(254) NOT NULL,
    recipient_name VARCHAR(200) NOT NULL,
    activity VARCHAR(500) NOT NULL,
    activity_date VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP NULL,
    claim_token VARCHAR(36) NULL,
    sent_at TIMESTAMP NULL,
    last_error VARCHAR(500) NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_due
    ON notification_outbox (status, next_attempt_at, created_at);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_claim
    ON notification_outbox (claim_token);
