package com.example.betting.risk.messaging;

/** Kafka {@code BetPlaced} 소비 페이로드 (Betting 발행). 필드는 발행자와 일치. */
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
