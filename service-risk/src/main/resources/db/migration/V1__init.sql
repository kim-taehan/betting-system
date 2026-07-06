CREATE TABLE user_exposure (
    user_id            VARCHAR(64) PRIMARY KEY,
    total_staked_minor BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE selection_exposure (
    selection_id       VARCHAR(64) PRIMARY KEY,
    event_id           VARCHAR(64) NOT NULL,
    market_id          VARCHAR(64) NOT NULL,
    total_staked_minor BIGINT NOT NULL DEFAULT 0
);
