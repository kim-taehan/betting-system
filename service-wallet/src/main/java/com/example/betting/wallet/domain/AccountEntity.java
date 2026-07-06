package com.example.betting.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class AccountEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    private String currency;

    /** 잔액 (최소 화폐 단위 정수). */
    @Column(name = "balance_minor")
    private long balanceMinor;

    protected AccountEntity() {
    }

    public String getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public void setBalanceMinor(long balanceMinor) {
        this.balanceMinor = balanceMinor;
    }
}
