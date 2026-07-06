package com.example.betting.betting.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetSlipRepository extends JpaRepository<BetSlipEntity, String> {

    List<BetSlipEntity> findByUserId(String userId);

    /** 정산 대상: 특정 마켓의 미결(PENDING) 베팅. */
    List<BetSlipEntity> findByMarketIdAndStatus(String marketId, BetStatus status);
}
