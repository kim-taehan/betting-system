package com.example.betting.event.messaging;

/** Kafka {@code OddsAdjustment} 소비 페이로드 (Risk 발행). factor 만큼 배당 조정. */
public record OddsAdjustmentEvent(
        String eventId,
        String marketId,
        String selectionId,
        double factor,
        long timestampEpochMs
) {
    public static final String TOPIC = "OddsAdjustment";
}
