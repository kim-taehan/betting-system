-- 학습용 시드 데이터: 경기 2개, 각 1X2(승무패) 마켓 + 3개 셀렉션.

INSERT INTO event (id, name, sport, status, start_time) VALUES
    ('evt-1', 'Seoul FC vs Busan FC', 'SOCCER', 'SCHEDULED', NOW() + INTERVAL '1 hour'),
    ('evt-2', 'Incheon United vs Daegu FC', 'SOCCER', 'SCHEDULED', NOW() + INTERVAL '2 hour');

INSERT INTO market (id, event_id, name) VALUES
    ('mkt-1', 'evt-1', '1X2'),
    ('mkt-2', 'evt-2', '1X2');

INSERT INTO selection (id, market_id, name, odds) VALUES
    ('sel-1', 'mkt-1', 'HOME', 1.85),
    ('sel-2', 'mkt-1', 'DRAW', 3.40),
    ('sel-3', 'mkt-1', 'AWAY', 4.20),
    ('sel-4', 'mkt-2', 'HOME', 2.10),
    ('sel-5', 'mkt-2', 'DRAW', 3.10),
    ('sel-6', 'mkt-2', 'AWAY', 3.30);
