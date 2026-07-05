package com.example.betting.event.service;

import com.example.betting.event.domain.SelectionEntity;
import com.example.betting.event.domain.SelectionRepository;
import com.example.betting.event.messaging.OddsChangedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 뼈대 단계용 단순 배당 변동: 무작위 셀렉션 하나의 배당을 ±10% 흔든다.
 * (실제 배당 산출 모델은 웨이브 2에서 이 서비스 내부만 교체한다.)
 */
@Service
public class OddsUpdateService {

    private static final double MIN_ODDS = 1.01;

    private final SelectionRepository selectionRepository;

    public OddsUpdateService(SelectionRepository selectionRepository) {
        this.selectionRepository = selectionRepository;
    }

    /**
     * 무작위 셀렉션 하나의 배당을 갱신하고, 변경 내용을 반환한다.
     * 연관(market/event) 로드를 위해 트랜잭션 안에서 처리한다.
     */
    @Transactional
    public Optional<OddsChangedEvent> jitterRandomSelection() {
        List<SelectionEntity> selections = selectionRepository.findAll();
        if (selections.isEmpty()) {
            return Optional.empty();
        }
        SelectionEntity selection = selections.get(ThreadLocalRandom.current().nextInt(selections.size()));

        double factor = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2; // 0.9 ~ 1.1
        double newOdds = Math.max(MIN_ODDS, Math.round(selection.getOdds() * factor * 100.0) / 100.0);
        selection.setOdds(newOdds);

        return Optional.of(new OddsChangedEvent(
                selection.getMarket().getEvent().getId(),
                selection.getMarket().getId(),
                selection.getId(),
                newOdds,
                Instant.now().toEpochMilli()));
    }
}
