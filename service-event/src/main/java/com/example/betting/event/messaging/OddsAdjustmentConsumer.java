package com.example.betting.event.messaging;

import com.example.betting.event.service.OddsChangePublisher;
import com.example.betting.event.service.OddsUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Risk 의 OddsAdjustment 를 구독해 해당 셀렉션 배당을 조정하고 변동을 전파한다. */
@Component
public class OddsAdjustmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(OddsAdjustmentConsumer.class);

    private final OddsUpdateService oddsUpdateService;
    private final OddsChangePublisher oddsChangePublisher;

    public OddsAdjustmentConsumer(OddsUpdateService oddsUpdateService,
                                  OddsChangePublisher oddsChangePublisher) {
        this.oddsUpdateService = oddsUpdateService;
        this.oddsChangePublisher = oddsChangePublisher;
    }

    @KafkaListener(topics = OddsAdjustmentEvent.TOPIC)
    public void onOddsAdjustment(OddsAdjustmentEvent event) {
        oddsUpdateService.adjustOdds(event.selectionId(), event.factor()).ifPresent(change -> {
            oddsChangePublisher.publish(change);
            log.info("리스크 배당 조정: {} (×{}) → {}", change.selectionId(), event.factor(), change.odds());
        });
    }
}
