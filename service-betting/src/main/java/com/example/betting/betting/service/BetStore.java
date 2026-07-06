package com.example.betting.betting.service;

import com.example.betting.betting.domain.BetSlipEntity;
import com.example.betting.betting.domain.BetSlipRepository;
import com.example.betting.betting.domain.BetStatus;
import com.example.betting.betting.messaging.BetPlacedEvent;
import com.example.betting.betting.messaging.BetSettledEvent;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 베팅 상태의 트랜잭션 경계. DB 쓰기 + 도메인 이벤트 발행(커밋 후 Kafka)을 담당한다.
 * 외부 gRPC 호출(오케스트레이션)은 여기 두지 않는다(트랜잭션 밖).
 */
@Service
public class BetStore {

    private final BetSlipRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public BetStore(BetSlipRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /** 접수된 베팅을 PENDING 으로 저장하고 BetPlaced 를 발행. */
    @Transactional
    public BetSlipEntity savePlaced(BetSlipEntity slip) {
        BetSlipEntity saved = repository.save(slip);
        eventPublisher.publishEvent(new BetPlacedEvent(
                saved.getId(), saved.getUserId(), saved.getEventId(), saved.getMarketId(),
                saved.getSelectionId(), saved.getCurrency(), saved.getStakeMinor(),
                saved.getOdds(), saved.getPlacedAt().toEpochMilli()));
        return saved;
    }

    /**
     * 마켓 정산: 해당 마켓의 PENDING 베팅을 승/패 판정하고 BetSettled 를 발행한다.
     * 이미 정산된 베팅은 건드리지 않으므로 중복 EventSettled 소비에도 안전(멱등).
     */
    @Transactional
    public int settleMarket(String marketId, String winningSelectionId) {
        List<BetSlipEntity> pending = repository.findByMarketIdAndStatus(marketId, BetStatus.PENDING);
        for (BetSlipEntity bet : pending) {
            boolean won = bet.getSelectionId().equals(winningSelectionId);
            long payout = won ? Math.round(bet.getStakeMinor() * bet.getOdds()) : 0L;
            bet.settle(won ? BetStatus.WON : BetStatus.LOST, payout);
            eventPublisher.publishEvent(new BetSettledEvent(
                    bet.getId(), bet.getUserId(), bet.getCurrency(), payout,
                    won ? "WON" : "LOST"));
        }
        return pending.size();
    }

    @Transactional(readOnly = true)
    public BetSlipEntity get(String betId) {
        return repository.findById(betId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<BetSlipEntity> listByUser(String userId) {
        return repository.findByUserId(userId);
    }
}
