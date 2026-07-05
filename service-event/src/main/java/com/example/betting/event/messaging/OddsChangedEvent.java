package com.example.betting.event.messaging;

/**
 * Kafka 토픽 {@code OddsChanged} 페이로드 (JSON 직렬화).
 * 배당이 바뀐 셀렉션 1건을 나타낸다.
 */
public record OddsChangedEvent(
        String eventId,
        String marketId,
        String selectionId,
        double odds,
        long timestampEpochMs
) {
    public static final String TOPIC = "OddsChanged";
}
