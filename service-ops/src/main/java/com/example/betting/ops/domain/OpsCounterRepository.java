package com.example.betting.ops.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpsCounterRepository extends JpaRepository<OpsCounterEntity, String> {

    /** 카운터 원자적 증가 (행은 Flyway 시드로 미리 존재). */
    @Modifying
    @Query("update OpsCounterEntity c set c.value = c.value + :delta where c.name = :name")
    int increment(@Param("name") String name, @Param("delta") long delta);
}
