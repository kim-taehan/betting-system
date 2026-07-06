package com.example.betting.betting.messaging;

import com.example.betting.betting.service.BetStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 경기 정산 이벤트를 구독해 해당 마켓의 PENDING 베팅을 승/패 판정한다.
 * 이미 정산된 베팅은 건너뛰므로 중복 소비(at-least-once)에도 안전.
 */
@Component
public class EventSettledConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventSettledConsumer.class);

    private final BetStore betStore;

    public EventSettledConsumer(BetStore betStore) {
        this.betStore = betStore;
    }

    @KafkaListener(topics = EventSettledEvent.TOPIC)
    public void onEventSettled(EventSettledEvent event) {
        int settled = betStore.settleMarket(event.marketId(), event.winningSelectionId());
        if (settled > 0) {
            log.info("정산: market={} winner={} → {}건", event.marketId(), event.winningSelectionId(), settled);
        }
    }
}
