package com.example.betting.risk.messaging;

/** Kafka {@code RiskAlert} 발행 페이로드. Ops 가 구독해 모니터링/알림. */
public record RiskAlertEvent(
        String type,         // 예: SELECTION_EXPOSURE
        String subjectId,    // 예: selectionId
        String message,
        long timestampEpochMs
) {
    public static final String TOPIC = "RiskAlert";
}
