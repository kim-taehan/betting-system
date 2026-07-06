package com.example.betting.betting.messaging;

/**
 * Kafka 토픽 {@code EventSettled} 페이로드 (Event 서비스가 발행).
 * 해당 마켓의 승리 셀렉션이 정해졌음을 알린다 → Betting 이 베팅을 정산한다.
 */
public record EventSettledEvent(
        String eventId,
        String marketId,
        String winningSelectionId,
        long timestampEpochMs
) {
    public static final String TOPIC = "EventSettled";
}
