package com.example.betting.event.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "selection")
public class SelectionEntity {

    @Id
    private String id;

    private String name;

    private double odds;

    @ManyToOne
    @JoinColumn(name = "market_id")
    private MarketEntity market;

    protected SelectionEntity() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getOdds() {
        return odds;
    }

    public void setOdds(double odds) {
        this.odds = odds;
    }

    public MarketEntity getMarket() {
        return market;
    }
}
