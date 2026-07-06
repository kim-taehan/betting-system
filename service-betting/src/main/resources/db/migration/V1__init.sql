CREATE TABLE bet_slip (
    id            VARCHAR(64) PRIMARY KEY,
    user_id       VARCHAR(64) NOT NULL,
    event_id      VARCHAR(64) NOT NULL,
    market_id     VARCHAR(64) NOT NULL,
    selection_id  VARCHAR(64) NOT NULL,
    currency      VARCHAR(3)  NOT NULL,
    stake_minor   BIGINT      NOT NULL,
    odds          DOUBLE PRECISION NOT NULL,
    status        VARCHAR(20) NOT NULL,
    payout_minor  BIGINT      NOT NULL,
    placed_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_bet_user ON bet_slip (user_id);
CREATE INDEX idx_bet_market_status ON bet_slip (market_id, status);
