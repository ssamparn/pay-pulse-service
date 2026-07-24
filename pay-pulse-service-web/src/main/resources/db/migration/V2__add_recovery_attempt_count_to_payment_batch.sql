ALTER TABLE payment_batch
    ADD COLUMN IF NOT EXISTS recovery_attempt_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE payment_batch
    ADD CONSTRAINT chk_payment_batch_recovery_attempt_count_non_negative
        CHECK (recovery_attempt_count >= 0);

