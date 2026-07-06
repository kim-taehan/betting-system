package com.example.betting.betting.service;

import com.example.betting.betting.domain.BetSlipEntity;
import com.example.betting.proto.common.v1.Money;
import com.example.betting.proto.event.v1.EventServiceGrpc;
import com.example.betting.proto.event.v1.GetOddsRequest;
import com.example.betting.proto.event.v1.GetOddsResponse;
import com.example.betting.proto.event.v1.Market;
import com.example.betting.proto.event.v1.Selection;
import com.example.betting.proto.risk.v1.CheckBetRequest;
import com.example.betting.proto.risk.v1.CheckBetResponse;
import com.example.betting.proto.risk.v1.RiskServiceGrpc;
import com.example.betting.proto.wallet.v1.CreditRequest;
import com.example.betting.proto.wallet.v1.DebitRequest;
import com.example.betting.proto.wallet.v1.WalletServiceGrpc;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 베팅 오케스트레이터. PlaceBet 은 Event/Wallet 을 gRPC 로 동기 호출한다.
 * 외부 호출이 섞이므로 이 메서드는 트랜잭션 밖이며, DB 쓰기는 {@link BetStore} 에 위임한다.
 */
@Service
public class BettingService {

    private static final Logger log = LoggerFactory.getLogger(BettingService.class);

    private final EventServiceGrpc.EventServiceBlockingStub eventStub;
    private final RiskServiceGrpc.RiskServiceBlockingStub riskStub;
    private final WalletServiceGrpc.WalletServiceBlockingStub walletStub;
    private final BetStore betStore;

    public BettingService(EventServiceGrpc.EventServiceBlockingStub eventStub,
                          RiskServiceGrpc.RiskServiceBlockingStub riskStub,
                          WalletServiceGrpc.WalletServiceBlockingStub walletStub,
                          BetStore betStore) {
        this.eventStub = eventStub;
        this.riskStub = riskStub;
        this.walletStub = walletStub;
        this.betStore = betStore;
    }

    public BetSlipEntity placeBet(String userId, String eventId, String marketId,
                                  String selectionId, String currency, long stakeMinor) {
        // 1) Event.GetOdds (gRPC) — 존재 검증 + 접수 시점 배당 확보
        double odds = lookupOdds(eventId, marketId, selectionId);

        // 2) Risk.CheckBet (gRPC 동기 게이트) — 하드 한도 초과면 거절
        CheckBetResponse risk = riskStub.checkBet(CheckBetRequest.newBuilder()
                .setUserId(userId).setEventId(eventId).setMarketId(marketId)
                .setSelectionId(selectionId).setStake(money(currency, stakeMinor))
                .build());
        if (!risk.getApproved()) {
            throw new BetRejectedException(risk.getReason());
        }

        String betId = UUID.randomUUID().toString();

        // 3) Wallet.Debit (gRPC) — 잔액부족 등은 StatusRuntimeException 으로 전파(→ 거절)
        walletStub.debit(DebitRequest.newBuilder()
                .setUserId(userId)
                .setAmount(money(currency, stakeMinor))
                .setIdempotencyKey(betId)
                .setReference(betId)
                .build());

        // 4) 저장(PENDING) + BetPlaced 발행. 저장 실패 시 차감 보상.
        BetSlipEntity slip = new BetSlipEntity(betId, userId, eventId, marketId, selectionId,
                currency, stakeMinor, odds, Instant.now());
        try {
            return betStore.savePlaced(slip);
        } catch (RuntimeException ex) {
            compensateDebit(userId, currency, stakeMinor, betId);
            throw ex;
        }
    }

    public BetSlipEntity getBet(String betId) {
        return betStore.get(betId);
    }

    public List<BetSlipEntity> listBets(String userId) {
        return betStore.listByUser(userId);
    }

    private double lookupOdds(String eventId, String marketId, String selectionId) {
        GetOddsResponse response = eventStub.getOdds(GetOddsRequest.newBuilder()
                .setEventId(eventId).build());
        for (Market market : response.getEvent().getMarketsList()) {
            if (!market.getId().equals(marketId)) {
                continue;
            }
            for (Selection selection : market.getSelectionsList()) {
                if (selection.getId().equals(selectionId)) {
                    return selection.getOdds();
                }
            }
        }
        throw new BetRejectedException(
                "selection not found: event=" + eventId + " market=" + marketId + " selection=" + selectionId);
    }

    private void compensateDebit(String userId, String currency, long stakeMinor, String betId) {
        try {
            walletStub.credit(CreditRequest.newBuilder()
                    .setUserId(userId)
                    .setAmount(money(currency, stakeMinor))
                    .setIdempotencyKey("compensate:" + betId)
                    .setReference(betId)
                    .build());
            log.warn("베팅 저장 실패 → 차감 보상 완료: bet={}", betId);
        } catch (RuntimeException ce) {
            log.error("차감 보상 실패: bet={}", betId, ce);
        }
    }

    private static Money money(String currency, long amountMinor) {
        return Money.newBuilder().setCurrency(currency).setAmountMinor(amountMinor).build();
    }
}
