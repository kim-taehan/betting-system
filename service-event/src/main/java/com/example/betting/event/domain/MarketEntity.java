package com.example.betting.event.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "market")
public class MarketEntity {

    @Id
    private String id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private EventEntity event;

    @OneToMany(mappedBy = "market", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SelectionEntity> selections = new ArrayList<>();

    protected MarketEntity() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EventEntity getEvent() {
        return event;
    }

    public List<SelectionEntity> getSelections() {
        return selections;
    }
}
