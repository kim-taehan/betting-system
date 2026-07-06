package com.example.betting.wallet.messaging;

/**
 * Kafka 토픽 {@code BetSettled} 페이로드 (Betting 서비스가 발행, Phase 3).
 * Wallet 은 payout > 0 이면 당첨금을 Credit 한다. betId 로 멱등 처리.
 */
public record BetSettledEvent(
        String betId,
        String userId,
        String currency,
        long payoutMinor,
        String result          // WON / LOST
) {
    public static final String TOPIC = "BetSettled";
}
