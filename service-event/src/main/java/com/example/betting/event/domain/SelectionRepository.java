package com.example.betting.event.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SelectionRepository extends JpaRepository<SelectionEntity, String> {
}
