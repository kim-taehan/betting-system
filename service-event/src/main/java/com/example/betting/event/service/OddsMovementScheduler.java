package com.example.betting.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 주기적으로 배당을 흔들고 그 변동을 전파(스트림 + Kafka)한다. */
@Component
public class OddsMovementScheduler {

    private static final Logger log = LoggerFactory.getLogger(OddsMovementScheduler.class);

    private final OddsUpdateService oddsUpdateService;
    private final OddsChangePublisher oddsChangePublisher;

    public OddsMovementScheduler(OddsUpdateService oddsUpdateService,
                                 OddsChangePublisher oddsChangePublisher) {
        this.oddsUpdateService = oddsUpdateService;
        this.oddsChangePublisher = oddsChangePublisher;
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void moveOdds() {
        oddsUpdateService.jitterRandomSelection().ifPresent(change -> {
            oddsChangePublisher.publish(change);
            log.debug("배당 변동: {} → {}", change.selectionId(), change.odds());
        });
    }
}
