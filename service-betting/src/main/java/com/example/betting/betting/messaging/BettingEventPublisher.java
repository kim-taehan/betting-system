package com.example.betting.betting.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 도메인 이벤트를 트랜잭션 커밋 이후에 Kafka 로 발행 (미커밋 상태 미발행).
 * Wallet 의 WalletEventPublisher 와 동일한 패턴.
 */
@Component
public class BettingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BettingEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBetPlaced(BetPlacedEvent event) {
        kafkaTemplate.send(BetPlacedEvent.TOPIC, event.betId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBetSettled(BetSettledEvent event) {
        kafkaTemplate.send(BetSettledEvent.TOPIC, event.betId(), event);
    }
}
