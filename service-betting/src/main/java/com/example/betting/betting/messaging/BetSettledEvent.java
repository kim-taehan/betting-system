package com.example.betting.betting.messaging;

/**
 * Kafka 토픽 {@code BetSettled} 페이로드. Wallet(당첨금 Credit)·Ops(통계)가 구독.
 * 필드는 Wallet 의 소비 레코드와 일치해야 한다(betId/userId/currency/payoutMinor/result).
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
