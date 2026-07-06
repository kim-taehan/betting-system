-- 항상 베팅 가능한 데모 경기(SCHEDULED) 추가.
INSERT INTO event (id, name, sport, status, start_time) VALUES
    ('evt-3', 'Jeonbuk vs Ulsan', 'SOCCER', 'SCHEDULED', NOW() + INTERVAL '1 hour'),
    ('evt-4', 'Pohang vs Gangwon', 'SOCCER', 'SCHEDULED', NOW() + INTERVAL '3 hour'),
    ('evt-5', 'Suwon vs Gwangju', 'SOCCER', 'SCHEDULED', NOW() + INTERVAL '5 hour');

INSERT INTO market (id, event_id, name) VALUES
    ('mkt-3', 'evt-3', '1X2'),
    ('mkt-4', 'evt-4', '1X2'),
    ('mkt-5', 'evt-5', '1X2');

INSERT INTO selection (id, market_id, name, odds) VALUES
    ('sel-7',  'mkt-3', 'HOME', 2.05), ('sel-8',  'mkt-3', 'DRAW', 3.30), ('sel-9',  'mkt-3', 'AWAY', 3.40),
    ('sel-10', 'mkt-4', 'HOME', 1.75), ('sel-11', 'mkt-4', 'DRAW', 3.50), ('sel-12', 'mkt-4', 'AWAY', 4.60),
    ('sel-13', 'mkt-5', 'HOME', 2.40), ('sel-14', 'mkt-5', 'DRAW', 3.10), ('sel-15', 'mkt-5', 'AWAY', 2.90);
