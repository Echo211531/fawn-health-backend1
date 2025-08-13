package com.ljh.fawnhealth.service;

import com.ljh.fawnhealth.model.entity.HealthRiskWarning;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthRiskWarningService {

    void save(HealthRiskWarning warning);

    List<HealthRiskWarning> listByUser(Long userId);

    List<HealthRiskWarning> listByUserAndTimeRange(Long userId, LocalDateTime start, LocalDateTime end);

    List<HealthRiskWarning> listUnprocessed();

    List<HealthRiskWarning> getLatestUnprocessedByUser(Long userId, int i);
}