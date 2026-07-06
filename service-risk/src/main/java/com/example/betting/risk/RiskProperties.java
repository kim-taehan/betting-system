package com.example.betting.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 위험관리 한도/임계 설정 (application.yml 의 risk.* ). */
@ConfigurationProperties(prefix = "risk")
public record RiskProperties(
        long maxSingleStake,
        long maxUserExposure,
        long selectionAlertThreshold
) {
}
