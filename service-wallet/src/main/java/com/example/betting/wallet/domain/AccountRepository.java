package com.example.betting.wallet.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    /** 동시 차감/지급 경합을 막기 위해 계정 행을 비관적 락으로 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.userId = :userId")
    Optional<AccountEntity> findByIdForUpdate(@Param("userId") String userId);
}
