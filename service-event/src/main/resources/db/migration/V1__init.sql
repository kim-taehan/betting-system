CREATE TABLE event (
    id         VARCHAR(64) PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    sport      VARCHAR(50)  NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    start_time TIMESTAMPTZ  NOT NULL
);

CREATE TABLE market (
    id       VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL REFERENCES event (id),
    name     VARCHAR(100) NOT NULL
);

CREATE TABLE selection (
    id        VARCHAR(64) PRIMARY KEY,
    market_id VARCHAR(64) NOT NULL REFERENCES market (id),
    name      VARCHAR(100) NOT NULL,
    odds      DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_market_event ON market (event_id);
CREATE INDEX idx_selection_market ON selection (market_id);
