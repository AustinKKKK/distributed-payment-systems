CREATE TABLE IF NOT EXISTS payments (
    payment_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idempotency_key VARCHAR(256) NOT NULL UNIQUE,
    status VARCHAR(256) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    entry_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(payment_id),
    account_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ledger_payment ON ledger_entries(payment_id);
CREATE INDEX IF NOT EXISTS idx_ledger_account ON ledger_entries(account_id);