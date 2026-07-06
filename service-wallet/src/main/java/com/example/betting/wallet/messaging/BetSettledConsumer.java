package com.example.betting.wallet.messaging;

import com.example.betting.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 정산 이벤트를 구독해 당첨금을 비동기로 지급한다(동기 Debit 과 대비되는 비동기 Credit 경로).
 * betId 기반 멱등키로 중복 소비(at-least-once)를 안전하게 만든다.
 */
@Component
public class BetSettledConsumer {

    private static final Logger log = LoggerFactory.getLogger(BetSettledConsumer.class);

    private final WalletService walletService;

    public BetSettledConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = BetSettledEvent.TOPIC)
    public void onBetSettled(BetSettledEvent event) {
        if (event.payoutMinor() <= 0) {
            return; // 낙첨: 지급 없음
        }
        walletService.credit(
                event.userId(),
                event.payoutMinor(),
                "bet-settled:" + event.betId(),
                event.betId());
        log.info("정산 지급: bet={} user={} +{}", event.betId(), event.userId(), event.payoutMinor());
    }
}
