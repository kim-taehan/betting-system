package com.example.betting.event.messaging;

/**
 * Kafka 토픽 {@code EventSettled} 페이로드. Betting 이 구독해 베팅을 정산한다.
 * 필드는 Betting 의 소비 레코드와 일치해야 한다.
 */
public record EventSettledEvent(
        String eventId,
        String marketId,
        String winningSelectionId,
        long timestampEpochMs
) {
    public static final String TOPIC = "EventSettled";
}
