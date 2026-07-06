package com.example.betting.event.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** EventSettled 를 트랜잭션 커밋 이후에 Kafka 로 발행. */
@Component
public class EventEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventSettled(EventSettledEvent event) {
        kafkaTemplate.send(EventSettledEvent.TOPIC, event.eventId(), event);
    }
}
