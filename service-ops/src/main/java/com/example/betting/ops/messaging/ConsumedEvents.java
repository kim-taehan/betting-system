package com.example.betting.ops.messaging;

/**
 * Ops 가 소비하는 이벤트들의 (필요 필드만 담은) 레코드.
 * fail-on-unknown-properties=false 라 발행 필드의 부분집합이어도 역직렬화된다.
 */
final class ConsumedEvents {
    private ConsumedEvents() {
    }
}

record BetPlacedEvent(long stakeMinor) {
    static final String TOPIC = "BetPlaced";
}

record BetSettledEvent(long payoutMinor, String result) {
    static final String TOPIC = "BetSettled";
}

record RiskAlertEvent(String type, String subjectId, String message, long timestampEpochMs) {
    static final String TOPIC = "RiskAlert";
}
