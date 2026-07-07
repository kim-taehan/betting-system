CREATE TABLE ops_counter (
    name  VARCHAR(64) PRIMARY KEY,
    value BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE alert (
    id         VARCHAR(64) PRIMARY KEY,
    type       VARCHAR(50) NOT NULL,
    subject_id VARCHAR(64),
    message    VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_alert_created ON alert (created_at DESC);

-- 카운터 행을 미리 만들어 두어 원자적 UPDATE 증가가 항상 성립하게 한다.
INSERT INTO ops_counter (name, value) VALUES
    ('bets_placed', 0),
    ('bets_settled', 0),
    ('bets_won', 0),
    ('bets_lost', 0),
    ('total_staked_minor', 0),
    ('total_payout_minor', 0),
    ('wallet_changes', 0),
    ('risk_alerts', 0);
