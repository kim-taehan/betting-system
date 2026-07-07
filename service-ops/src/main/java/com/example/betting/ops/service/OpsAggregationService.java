package com.example.betting.ops.service;

import com.example.betting.ops.domain.AlertEntity;
import com.example.betting.ops.domain.AlertRepository;
import com.example.betting.ops.domain.OpsCounterRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트 소비 집계. DB 카운터(GetSystemStatus 용) + Micrometer 카운터(Prometheus/Grafana 용)를 함께 갱신한다.
 * 학습용: at-least-once 재전달 시 근사 집계(정확 카운트 아님).
 */
@Service
public class OpsAggregationService {

    private final OpsCounterRepository counters;
    private final AlertRepository alerts;
    private final MeterRegistry meter;

    public OpsAggregationService(OpsCounterRepository counters, AlertRepository alerts, MeterRegistry meter) {
        this.counters = counters;
        this.alerts = alerts;
        this.meter = meter;
    }

    @Transactional
    public void onBetPlaced(long stakeMinor) {
        counters.increment("bets_placed", 1);
        counters.increment("total_staked_minor", stakeMinor);
        meter.counter("ops.bets.placed").increment();
    }

    @Transactional
    public void onBetSettled(long payoutMinor, String result) {
        counters.increment("bets_settled", 1);
        boolean won = "WON".equals(result);
        counters.increment(won ? "bets_won" : "bets_lost", 1);
        if (won) {
            counters.increment("total_payout_minor", payoutMinor);
        }
        meter.counter("ops.bets.settled", "result", won ? "won" : "lost").increment();
    }

    @Transactional
    public void onWalletChanged() {
        counters.increment("wallet_changes", 1);
        meter.counter("ops.wallet.changes").increment();
    }

    @Transactional
    public void onRiskAlert(String type, String subjectId, String message, long timestampEpochMs) {
        counters.increment("risk_alerts", 1);
        alerts.save(new AlertEntity(UUID.randomUUID().toString(), type, subjectId, message,
                Instant.ofEpochMilli(timestampEpochMs)));
        meter.counter("ops.risk.alerts").increment();
    }
}
