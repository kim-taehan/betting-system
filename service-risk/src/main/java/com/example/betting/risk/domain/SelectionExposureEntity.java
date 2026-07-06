package com.example.betting.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 셀렉션별 누적 익스포저. 임계 초과 시 배당 조정/알림의 기준. */
@Entity
@Table(name = "selection_exposure")
public class SelectionExposureEntity {

    @Id
    @Column(name = "selection_id")
    private String selectionId;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "market_id")
    private String marketId;

    @Column(name = "total_staked_minor")
    private long totalStakedMinor;

    protected SelectionExposureEntity() {
    }

    public SelectionExposureEntity(String selectionId, String eventId, String marketId, long totalStakedMinor) {
        this.selectionId = selectionId;
        this.eventId = eventId;
        this.marketId = marketId;
        this.totalStakedMinor = totalStakedMinor;
    }

    public void add(long amountMinor) {
        this.totalStakedMinor += amountMinor;
    }

    public String getSelectionId() {
        return selectionId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getMarketId() {
        return marketId;
    }

    public long getTotalStakedMinor() {
        return totalStakedMinor;
    }
}
