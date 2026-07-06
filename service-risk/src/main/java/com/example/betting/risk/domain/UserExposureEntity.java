package com.example.betting.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 유저별 누적 익스포저(확정 베팅 스테이크 합). BetPlaced 소비로 갱신된다. */
@Entity
@Table(name = "user_exposure")
public class UserExposureEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "total_staked_minor")
    private long totalStakedMinor;

    protected UserExposureEntity() {
    }

    public UserExposureEntity(String userId, long totalStakedMinor) {
        this.userId = userId;
        this.totalStakedMinor = totalStakedMinor;
    }

    public void add(long amountMinor) {
        this.totalStakedMinor += amountMinor;
    }

    public String getUserId() {
        return userId;
    }

    public long getTotalStakedMinor() {
        return totalStakedMinor;
    }
}
