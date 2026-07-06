CREATE TABLE account (
    user_id       VARCHAR(64) PRIMARY KEY,
    currency      VARCHAR(3)  NOT NULL,
    balance_minor BIGINT      NOT NULL
);

CREATE TABLE ledger_entry (
    id                  VARCHAR(64) PRIMARY KEY,
    user_id             VARCHAR(64) NOT NULL REFERENCES account (user_id),
    type                VARCHAR(10) NOT NULL,
    amount_minor        BIGINT      NOT NULL,
    balance_after_minor BIGINT      NOT NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    reference           VARCHAR(128),
    created_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ledger_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_ledger_user ON ledger_entry (user_id);
