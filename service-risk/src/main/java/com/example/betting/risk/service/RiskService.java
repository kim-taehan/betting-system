package com.example.betting.risk.service;

import com.example.betting.risk.RiskProperties;
import com.example.betting.risk.domain.SelectionExposureEntity;
import com.example.betting.risk.domain.SelectionExposureRepository;
import com.example.betting.risk.domain.UserExposureEntity;
import com.example.betting.risk.domain.UserExposureRepository;
import com.example.betting.risk.messaging.BetPlacedEvent;
import com.example.betting.risk.messaging.OddsAdjustmentEvent;
import com.example.betting.risk.messaging.RiskAlertEvent;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 혼합 위험관리.
 * - 동기(checkBet): 하드 한도(단건 스테이크, 유저 누적 익스포저)를 접수 순간 검사 → 승인/거절.
 * - 비동기(applyBetPlaced): 확정 베팅으로 익스포저를 집계하고, 셀렉션 임계 초과 시
 *   OddsAdjustment(배당↓) + RiskAlert 를 발행.
 */
@Service
public class RiskService {

    /** 배당 조정 강도(익스포저 과다 셀렉션 배당을 10% 낮춘다). */
    private static final double ADJUST_FACTOR = 0.90;

    private final UserExposureRepository userExposureRepository;
    private final SelectionExposureRepository selectionExposureRepository;
    private final RiskProperties props;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meter;

    public RiskService(UserExposureRepository userExposureRepository,
                       SelectionExposureRepository selectionExposureRepository,
                       RiskProperties props,
                       ApplicationEventPublisher eventPublisher,
                       MeterRegistry meter) {
        this.userExposureRepository = userExposureRepository;
        this.selectionExposureRepository = selectionExposureRepository;
        this.props = props;
        this.eventPublisher = eventPublisher;
        this.meter = meter;
    }

    /** 동기 하드 한도 게이트. */
    @Transactional(readOnly = true)
    public CheckResult checkBet(String userId, long stakeMinor) {
        CheckResult result = evaluate(userId, stakeMinor);
        // Risk 거절율 메트릭 (Grafana)
        meter.counter("risk.checks", "result", result.approved() ? "approved" : "rejected").increment();
        return result;
    }

    private CheckResult evaluate(String userId, long stakeMinor) {
        if (stakeMinor > props.maxSingleStake()) {
            return CheckResult.reject("단건 스테이크 한도 초과 (max " + props.maxSingleStake() + ")");
        }
        long current = userExposureRepository.findById(userId)
                .map(UserExposureEntity::getTotalStakedMinor).orElse(0L);
        if (current + stakeMinor > props.maxUserExposure()) {
            return CheckResult.reject("유저 누적 익스포저 한도 초과 (현재 " + current
                    + ", max " + props.maxUserExposure() + ")");
        }
        return CheckResult.approve();
    }

    @Transactional(readOnly = true)
    public long userExposure(String userId) {
        return userExposureRepository.findById(userId)
                .map(UserExposureEntity::getTotalStakedMinor).orElse(0L);
    }

    /** 비동기 집계 + 이상탐지. 확정 베팅(BetPlaced)마다 호출. */
    @Transactional
    public void applyBetPlaced(BetPlacedEvent bet) {
        UserExposureEntity user = userExposureRepository.findById(bet.userId())
                .orElseGet(() -> new UserExposureEntity(bet.userId(), 0));
        user.add(bet.stakeMinor());
        userExposureRepository.save(user);

        SelectionExposureEntity selection = selectionExposureRepository.findById(bet.selectionId())
                .orElseGet(() -> new SelectionExposureEntity(
                        bet.selectionId(), bet.eventId(), bet.marketId(), 0));
        long before = selection.getTotalStakedMinor();
        selection.add(bet.stakeMinor());
        selectionExposureRepository.save(selection);

        // 임계를 "넘는 순간" 한 번만 조정/알림 (매 베팅마다 반복 조정 방지)
        long threshold = props.selectionAlertThreshold();
        if (before <= threshold && selection.getTotalStakedMinor() > threshold) {
            long now = Instant.now().toEpochMilli();
            eventPublisher.publishEvent(new OddsAdjustmentEvent(
                    bet.eventId(), bet.marketId(), bet.selectionId(), ADJUST_FACTOR, now));
            eventPublisher.publishEvent(new RiskAlertEvent(
                    "SELECTION_EXPOSURE", bet.selectionId(),
                    "셀렉션 익스포저 임계 초과: " + selection.getTotalStakedMinor()
                            + " > " + threshold + " → 배당 조정", now));
        }
    }

    /** checkBet 결과. */
    public record CheckResult(boolean approved, String reason) {
        public static CheckResult approve() {
            return new CheckResult(true, "");
        }

        public static CheckResult reject(String reason) {
            return new CheckResult(false, reason);
        }
    }
}
