package com.example.betting.event.service;

import com.example.betting.event.grpc.OddsBroadcaster;
import com.example.betting.event.messaging.OddsChangedEvent;
import com.example.betting.proto.event.v1.OddsUpdate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** 배당 변동 1건을 StreamOdds 구독자 브로드캐스트 + Kafka OddsChanged 발행으로 전파. */
@Component
public class OddsChangePublisher {

    private final OddsBroadcaster broadcaster;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OddsChangePublisher(OddsBroadcaster broadcaster, KafkaTemplate<String, Object> kafkaTemplate) {
        this.broadcaster = broadcaster;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OddsChangedEvent change) {
        broadcaster.broadcast(OddsUpdate.newBuilder()
                .setEventId(change.eventId())
                .setMarketId(change.marketId())
                .setSelectionId(change.selectionId())
                .setOdds(change.odds())
                .setTimestampEpochMs(change.timestampEpochMs())
                .build());
        kafkaTemplate.send(OddsChangedEvent.TOPIC, change.selectionId(), change);
    }
}
