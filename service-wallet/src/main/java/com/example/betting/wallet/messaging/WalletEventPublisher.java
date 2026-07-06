package com.example.betting.wallet.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * WalletChanged 를 트랜잭션 커밋 이후에 Kafka 로 발행한다.
 * WalletService 는 {@code ApplicationEventPublisher} 로 도메인 이벤트만 던지고,
 * 여기서 커밋 성공 시에만 실제 발행 → 미커밋 상태가 흘러가지 않는다.
 */
@Component
public class WalletEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WalletEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWalletChanged(WalletChangedEvent event) {
        kafkaTemplate.send(WalletChangedEvent.TOPIC, event.userId(), event);
    }
}
