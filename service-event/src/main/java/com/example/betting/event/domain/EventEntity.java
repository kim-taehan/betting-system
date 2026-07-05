package com.example.betting.event.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event")
public class EventEntity {

    @Id
    private String id;

    private String name;

    private String sport;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MarketEntity> markets = new ArrayList<>();

    protected EventEntity() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSport() {
        return sport;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public List<MarketEntity> getMarkets() {
        return markets;
    }
}
