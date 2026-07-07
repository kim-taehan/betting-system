package com.example.betting.ops.messaging;

import com.example.betting.ops.service.OpsAggregationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 전 이벤트를 구독해 운영 집계로 흘린다.
 * 여러 타입을 한 서비스에서 소비하므로 값은 String 으로 받아 리스너별로 각 타입으로 파싱한다.
 */
@Component
public class OpsEventConsumer {

    private final JsonMapper jsonMapper;
    private final OpsAggregationService aggregation;

    public OpsEventConsumer(JsonMapper jsonMapper, OpsAggregationService aggregation) {
        this.jsonMapper = jsonMapper;
        this.aggregation = aggregation;
    }

    @KafkaListener(topics = BetPlacedEvent.TOPIC)
    public void onBetPlaced(String json) {
        BetPlacedEvent e = jsonMapper.readValue(json, BetPlacedEvent.class);
        aggregation.onBetPlaced(e.stakeMinor());
    }

    @KafkaListener(topics = BetSettledEvent.TOPIC)
    public void onBetSettled(String json) {
        BetSettledEvent e = jsonMapper.readValue(json, BetSettledEvent.class);
        aggregation.onBetSettled(e.payoutMinor(), e.result());
    }

    @KafkaListener(topics = "WalletChanged")
    public void onWalletChanged(String json) {
        aggregation.onWalletChanged();
    }

    @KafkaListener(topics = RiskAlertEvent.TOPIC)
    public void onRiskAlert(String json) {
        RiskAlertEvent e = jsonMapper.readValue(json, RiskAlertEvent.class);
        aggregation.onRiskAlert(e.type(), e.subjectId(), e.message(), e.timestampEpochMs());
    }
}
