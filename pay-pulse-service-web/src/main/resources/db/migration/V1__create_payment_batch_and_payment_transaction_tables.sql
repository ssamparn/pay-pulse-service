CREATE TABLE IF NOT EXISTS payment_batch (
    batch_id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    external_batch_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    execution_date DATE NOT NULL,
    batch_description VARCHAR(500) NOT NULL,
    requested_by VARCHAR(320) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    payments_count INTEGER NOT NULL,
    total_transactions INTEGER NOT NULL,
    successful_transactions INTEGER NOT NULL,
    failed_transactions INTEGER NOT NULL,
    pending_transactions INTEGER NOT NULL,
    progress_percentage INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_payment_batch_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_payment_batch_external_batch_id UNIQUE (external_batch_id),
    CONSTRAINT chk_payment_batch_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'PARTIALLY_COMPLETED')),
    CONSTRAINT chk_payment_batch_payment_method CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'SEPA', 'ACH', 'WALLET')),
    CONSTRAINT chk_payment_batch_total_amount_non_negative CHECK (total_amount >= 0),
    CONSTRAINT chk_payment_batch_currency_uppercase CHECK (currency = UPPER(currency)),
    CONSTRAINT chk_payment_batch_payments_count_non_negative CHECK (payments_count >= 0),
    CONSTRAINT chk_payment_batch_total_transactions_non_negative CHECK (total_transactions >= 0),
    CONSTRAINT chk_payment_batch_successful_transactions_non_negative CHECK (successful_transactions >= 0),
    CONSTRAINT chk_payment_batch_failed_transactions_non_negative CHECK (failed_transactions >= 0),
    CONSTRAINT chk_payment_batch_pending_transactions_non_negative CHECK (pending_transactions >= 0),
    CONSTRAINT chk_payment_batch_progress_percentage_range CHECK (progress_percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_payment_batch_transaction_totals CHECK (successful_transactions + failed_transactions + pending_transactions = total_transactions),
    CONSTRAINT chk_payment_batch_completed_at_terminal_state CHECK (
        (status IN ('COMPLETED', 'FAILED', 'PARTIALLY_COMPLETED') AND completed_at IS NOT NULL)
        OR (status IN ('PENDING', 'PROCESSING') AND completed_at IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_payment_batch_status_created_at
    ON payment_batch (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_batch_created_at
    ON payment_batch (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_batch_merchant_customer
    ON payment_batch (merchant_id, customer_id);

CREATE TABLE IF NOT EXISTS payment_transaction (
    payment_id VARCHAR(64) PRIMARY KEY,
    beneficiary_id VARCHAR(64) NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    beneficiary_name VARCHAR(255) NOT NULL,
    beneficiary_iban VARCHAR(34) NOT NULL,
    external_payment_id VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_reference VARCHAR(255) NOT NULL,
    failure_reason VARCHAR(500) NULL,
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_transaction_batch
        FOREIGN KEY (batch_id)
        REFERENCES payment_batch (batch_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_payment_transaction_batch_external_payment UNIQUE (batch_id, external_payment_id),
    CONSTRAINT chk_payment_transaction_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'PARTIALLY_COMPLETED')),
    CONSTRAINT chk_payment_transaction_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payment_transaction_currency_uppercase CHECK (currency = UPPER(currency)),
    CONSTRAINT chk_payment_transaction_processed_at_consistency CHECK (
        (status IN ('COMPLETED', 'FAILED', 'PARTIALLY_COMPLETED') AND processed_at IS NOT NULL)
        OR (status IN ('PENDING', 'PROCESSING') AND processed_at IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_batch_id
    ON payment_transaction (batch_id);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_batch_status
    ON payment_transaction (batch_id, status);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_created_at
    ON payment_transaction (created_at DESC);

