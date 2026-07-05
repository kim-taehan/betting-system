package com.example.betting.event.domain;

/** 경기 상태. proto EventStatus 와 매핑된다. */
public enum EventStatus {
    SCHEDULED,
    LIVE,
    SETTLED
}
