package com.example.betting.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 원장 기록 1건. idempotency_key 로 중복 요청을 방지한다(유니크). */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    private LedgerType type;

    @Column(name = "amount_minor")
    private long amountMinor;

    @Column(name = "balance_after_minor")
    private long balanceAfterMinor;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    private String reference;

    @Column(name = "created_at")
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(String id, String userId, LedgerType type, long amountMinor,
                       long balanceAfterMinor, String idempotencyKey, String reference, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amountMinor = amountMinor;
        this.balanceAfterMinor = balanceAfterMinor;
        this.idempotencyKey = idempotencyKey;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public LedgerType getType() {
        return type;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public long getBalanceAfterMinor() {
        return balanceAfterMinor;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
