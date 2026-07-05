package com.example.betting.event.service;

import com.example.betting.event.grpc.OddsBroadcaster;
import com.example.betting.event.messaging.OddsChangedEvent;
import com.example.betting.proto.event.v1.OddsUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 배당을 흔들고, 그 결과를 (1) Kafka {@code OddsChanged} 발행,
 * (2) StreamOdds 구독자에게 브로드캐스트 한다.
 */
@Component
public class OddsMovementScheduler {

    private static final Logger log = LoggerFactory.getLogger(OddsMovementScheduler.class);

    private final OddsUpdateService oddsUpdateService;
    private final OddsBroadcaster broadcaster;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OddsMovementScheduler(OddsUpdateService oddsUpdateService,
                                 OddsBroadcaster broadcaster,
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.oddsUpdateService = oddsUpdateService;
        this.broadcaster = broadcaster;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void moveOdds() {
        oddsUpdateService.jitterRandomSelection().ifPresent(change -> {
            // 스트림 구독자에게 먼저 밀어준다 (Kafka 장애가 스트리밍을 막지 않도록)
            broadcaster.broadcast(toUpdate(change));
            kafkaTemplate.send(OddsChangedEvent.TOPIC, change.selectionId(), change);
            log.debug("배당 변동: {} → {}", change.selectionId(), change.odds());
        });
    }

    private static OddsUpdate toUpdate(OddsChangedEvent change) {
        return OddsUpdate.newBuilder()
                .setEventId(change.eventId())
                .setMarketId(change.marketId())
                .setSelectionId(change.selectionId())
                .setOdds(change.odds())
                .setTimestampEpochMs(change.timestampEpochMs())
                .build();
    }
}
