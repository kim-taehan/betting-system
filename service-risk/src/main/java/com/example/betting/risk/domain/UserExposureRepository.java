package com.example.betting.risk.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExposureRepository extends JpaRepository<UserExposureEntity, String> {
}
