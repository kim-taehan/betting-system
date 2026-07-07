package com.example.betting.ops.service;

import com.example.betting.ops.domain.AlertEntity;
import com.example.betting.ops.domain.AlertRepository;
import com.example.betting.ops.domain.OpsCounterEntity;
import com.example.betting.ops.domain.OpsCounterRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpsQueryService {

    private final OpsCounterRepository counters;
    private final AlertRepository alerts;

    public OpsQueryService(OpsCounterRepository counters, AlertRepository alerts) {
        this.counters = counters;
        this.alerts = alerts;
    }

    public Map<String, Long> counters() {
        return counters.findAll().stream()
                .collect(Collectors.toMap(OpsCounterEntity::getName, OpsCounterEntity::getValue));
    }

    public List<AlertEntity> recentAlerts(int limit) {
        return alerts.findAllByOrderByCreatedAtDesc(Limit.of(limit > 0 ? limit : 20));
    }
}
