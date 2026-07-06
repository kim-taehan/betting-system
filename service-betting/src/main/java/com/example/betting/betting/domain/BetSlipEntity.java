package com.example.betting.betting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "bet_slip")
public class BetSlipEntity {

    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "market_id")
    private String marketId;

    @Column(name = "selection_id")
    private String selectionId;

    private String currency;

    @Column(name = "stake_minor")
    private long stakeMinor;

    /** 접수 시점 배당. */
    private double odds;

    @Enumerated(EnumType.STRING)
    private BetStatus status;

    /** 정산 후 지급액 (WON 이면 stake*odds, 아니면 0). */
    @Column(name = "payout_minor")
    private long payoutMinor;

    @Column(name = "placed_at")
    private Instant placedAt;

    protected BetSlipEntity() {
    }

    public BetSlipEntity(String id, String userId, String eventId, String marketId, String selectionId,
                         String currency, long stakeMinor, double odds, Instant placedAt) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.marketId = marketId;
        this.selectionId = selectionId;
        this.currency = currency;
        this.stakeMinor = stakeMinor;
        this.odds = odds;
        this.status = BetStatus.PENDING;
        this.payoutMinor = 0;
        this.placedAt = placedAt;
    }

    public void settle(BetStatus status, long payoutMinor) {
        this.status = status;
        this.payoutMinor = payoutMinor;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getMarketId() {
        return marketId;
    }

    public String getSelectionId() {
        return selectionId;
    }

    public String getCurrency() {
        return currency;
    }

    public long getStakeMinor() {
        return stakeMinor;
    }

    public double getOdds() {
        return odds;
    }

    public BetStatus getStatus() {
        return status;
    }

    public long getPayoutMinor() {
        return payoutMinor;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }
}
