package com.example.betting.risk.messaging;

import com.example.betting.risk.service.RiskService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** BetPlaced 를 구독해 익스포저를 집계하고 이상탐지를 수행한다(비동기 감시). */
@Component
public class BetPlacedConsumer {

    private final RiskService riskService;

    public BetPlacedConsumer(RiskService riskService) {
        this.riskService = riskService;
    }

    @KafkaListener(topics = BetPlacedEvent.TOPIC)
    public void onBetPlaced(BetPlacedEvent event) {
        riskService.applyBetPlaced(event);
    }
}
