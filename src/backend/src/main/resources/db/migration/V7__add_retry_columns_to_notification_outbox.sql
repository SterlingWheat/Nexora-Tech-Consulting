-- V7: Asegurar índices para consulta y reintentos en notification_outbox
CREATE INDEX IF NOT EXISTS idx_notification_outbox_retry_due
    ON public.notification_outbox (status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'RETRY', 'PROCESSING');

CREATE INDEX IF NOT EXISTS idx_notification_outbox_recipient_status
    ON public.notification_outbox (LOWER(recipient), status, created_at DESC);