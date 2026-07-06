package com.example.betting.wallet.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, String> {

    /** 멱등 처리: 같은 키가 이미 적용됐는지 조회. */
    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);
}
