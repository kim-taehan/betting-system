package com.example.betting.ops.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 운영 집계 카운터 (name → value). 이벤트 소비로 원자적 증가된다. */
@Entity
@Table(name = "ops_counter")
public class OpsCounterEntity {

    @Id
    private String name;

    private long value;

    protected OpsCounterEntity() {
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return value;
    }
}
