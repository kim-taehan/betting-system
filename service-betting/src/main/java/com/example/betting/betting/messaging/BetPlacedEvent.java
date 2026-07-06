package com.example.betting.betting.messaging;

/** Kafka 토픽 {@code BetPlaced} 페이로드. Risk(익스포저 집계)·Ops(통계)가 구독. */
public record BetPlacedEvent(
        String betId,
        String userId,
        String eventId,
        String marketId,
        String selectionId,
        String currency,
        long stakeMinor,
        double odds,
        long timestampEpochMs
) {
    public static final String TOPIC = "BetPlaced";
}
