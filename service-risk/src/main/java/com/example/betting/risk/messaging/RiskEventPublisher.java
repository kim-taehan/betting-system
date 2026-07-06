package com.example.betting.risk.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** RiskAlert / OddsAdjustment 를 트랜잭션 커밋 이후 Kafka 로 발행. */
@Component
public class RiskEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RiskEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOddsAdjustment(OddsAdjustmentEvent event) {
        kafkaTemplate.send(OddsAdjustmentEvent.TOPIC, event.selectionId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRiskAlert(RiskAlertEvent event) {
        kafkaTemplate.send(RiskAlertEvent.TOPIC, event.subjectId(), event);
    }
}
