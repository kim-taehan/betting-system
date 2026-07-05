package com.example.betting.event.service;

import com.example.betting.event.domain.EventEntity;
import com.example.betting.event.domain.EventRepository;
import com.example.betting.event.domain.EventStatus;
import com.example.betting.event.domain.MarketEntity;
import com.example.betting.event.domain.SelectionEntity;
import com.example.betting.proto.event.v1.Event;
import com.example.betting.proto.event.v1.Market;
import com.example.betting.proto.event.v1.Selection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경기/배당 조회 + 엔티티 → proto 매핑. LAZY 연관을 트랜잭션 안에서 로드해 매핑한다.
 */
@Service
@Transactional(readOnly = true)
public class EventQueryService {

    private final EventRepository eventRepository;

    public EventQueryService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /** status == null 이면 전체 조회. */
    public List<Event> listEvents(EventStatus status) {
        List<EventEntity> events = (status == null)
                ? eventRepository.findAll()
                : eventRepository.findByStatus(status);
        return events.stream().map(EventQueryService::toProto).toList();
    }

    public Optional<Event> getEvent(String eventId) {
        return eventRepository.findById(eventId).map(EventQueryService::toProto);
    }

    private static Event toProto(EventEntity e) {
        return Event.newBuilder()
                .setId(e.getId())
                .setName(e.getName())
                .setSport(e.getSport())
                .setStatus(toProtoStatus(e.getStatus()))
                .setStartTimeEpochMs(e.getStartTime().toEpochMilli())
                .addAllMarkets(e.getMarkets().stream().map(EventQueryService::toProto).toList())
                .build();
    }

    private static Market toProto(MarketEntity m) {
        return Market.newBuilder()
                .setId(m.getId())
                .setName(m.getName())
                .addAllSelections(m.getSelections().stream().map(EventQueryService::toProto).toList())
                .build();
    }

    private static Selection toProto(SelectionEntity s) {
        return Selection.newBuilder()
                .setId(s.getId())
                .setName(s.getName())
                .setOdds(s.getOdds())
                .build();
    }

    private static com.example.betting.proto.event.v1.EventStatus toProtoStatus(EventStatus status) {
        return com.example.betting.proto.event.v1.EventStatus.valueOf(status.name());
    }
}
