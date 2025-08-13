package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljh.fawnhealth.mapper.HealthRiskWarningMapper;
import com.ljh.fawnhealth.model.entity.HealthRiskWarning;
import com.ljh.fawnhealth.service.HealthRiskWarningService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthRiskWarningServiceImpl implements HealthRiskWarningService {

    @Resource
    private  HealthRiskWarningMapper mapper;

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

    @Override
    public List<HealthRiskWarning> getLatestUnprocessedByUser(Long userId, int limit) {
        LambdaQueryWrapper<HealthRiskWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRiskWarning::getUserId, userId)
                .eq(HealthRiskWarning::getStatus, 0)  // 0表示未处理
                .orderByDesc(HealthRiskWarning::getTriggerTime)  // 按触发时间倒序
                .last("LIMIT " + limit);  // 限制返回数量
        return mapper.selectList(wrapper);
    }
}