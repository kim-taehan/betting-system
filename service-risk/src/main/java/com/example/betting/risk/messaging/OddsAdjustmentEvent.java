package com.example.betting.risk.messaging;

/**
 * Kafka {@code OddsAdjustment} 발행 페이로드. Event/Odds 가 구독해 해당 셀렉션 배당을 조정한다.
 * factor < 1 이면 배당을 낮춰(익스포저 과다 셀렉션) 추가 베팅을 억제.
 */
public record OddsAdjustmentEvent(
        String eventId,
        String marketId,
        String selectionId,
        double factor,
        long timestampEpochMs
) {
    public static final String TOPIC = "OddsAdjustment";
}
