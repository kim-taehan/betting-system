package com.example.betting.event.service;

import com.example.betting.event.domain.EventEntity;
import com.example.betting.event.domain.EventRepository;
import com.example.betting.event.messaging.EventSettledEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 경기 정산: 상태를 SETTLED 로 바꾸고 EventSettled 를 (커밋 후) 발행. */
@Service
public class EventSettleService {

    private final EventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EventSettleService(EventRepository eventRepository, ApplicationEventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void settle(String eventId, String marketId, String winningSelectionId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("event not found: " + eventId));
        event.markSettled();
        eventPublisher.publishEvent(new EventSettledEvent(
                eventId, marketId, winningSelectionId, Instant.now().toEpochMilli()));
    }
}
