package com.example.betting.betting.service;

/** 베팅 접수 거절 (검증 실패 등). gRPC 계층에서 상태로 매핑. */
public class BetRejectedException extends RuntimeException {
    public BetRejectedException(String message) {
        super(message);
    }
}
