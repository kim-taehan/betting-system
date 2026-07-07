package com.example.betting.ops.domain;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, String> {

    List<AlertEntity> findAllByOrderByCreatedAtDesc(Limit limit);
}
