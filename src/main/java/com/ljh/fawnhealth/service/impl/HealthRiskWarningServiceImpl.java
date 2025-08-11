package com.ljh.fawnhealth.service.impl;

import com.ljh.fawnhealth.mapper.HealthRiskWarningMapper;
import com.ljh.fawnhealth.model.entity.HealthRiskWarning;
import com.ljh.fawnhealth.service.HealthRiskWarningService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthRiskWarningServiceImpl implements HealthRiskWarningService {

    private final HealthRiskWarningMapper mapper;

    public HealthRiskWarningServiceImpl(HealthRiskWarningMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(HealthRiskWarning warning) {
        mapper.insert(warning);
    }

    @Override
    public List<HealthRiskWarning> listByUser(Long userId) {
        return mapper.selectByUserId(userId);
    }

    @Override
    public List<HealthRiskWarning> listByUserAndTimeRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return mapper.selectByUserIdAndTimeRange(userId, start, end);
    }

    @Override
    public List<HealthRiskWarning> listUnprocessed() {
        return mapper.selectUnprocessedWarnings();
    }
}