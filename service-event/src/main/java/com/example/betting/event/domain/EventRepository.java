package com.example.betting.event.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, String> {

    List<EventEntity> findByStatus(EventStatus status);
}
