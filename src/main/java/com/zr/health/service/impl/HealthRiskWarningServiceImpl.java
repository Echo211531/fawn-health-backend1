package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.mapper.HealthRiskWarningMapper;
import com.zr.health.model.dto.healthWarning.HealthWarningAddDTO;
import com.zr.health.model.dto.healthWarning.HealthWarningPageQueryDTO;
import com.zr.health.model.dto.healthWarning.HealthWarningUpdateDTO;
import com.zr.health.model.entity.HealthRiskWarning;
import com.zr.health.service.HealthRiskWarningService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthRiskWarningServiceImpl implements HealthRiskWarningService {

    @Resource
    private HealthRiskWarningMapper mapper;

    @Override
    public void save(HealthRiskWarning warning) {
        // 入库前去重：同一用户、相同干预内容且未处理的记录存在，则不插入
        if (warning.getUserId() != null && warning.getInterventionContent() != null) {
            int exists = mapper.countUnprocessedByUserAndIntervention(warning.getUserId(),
                    warning.getInterventionContent());
            if (exists > 0) {
                return;
            }
        }
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
    public boolean markProcessed(Long id) {
        HealthRiskWarning update = new HealthRiskWarning();
        update.setId(id);
        update.setStatus(1);
        update.setProcessTime(LocalDateTime.now());
        return mapper.updateById(update) > 0;
    }

    @Override
    public List<HealthRiskWarning> getLatestUnprocessedByUser(Long userId, int limit) {
        LambdaQueryWrapper<HealthRiskWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRiskWarning::getUserId, userId)
                .eq(HealthRiskWarning::getStatus, 0)
                .orderByDesc(HealthRiskWarning::getTriggerTime)
                .last("LIMIT " + limit);
        return mapper.selectList(wrapper);
    }

    @Override
    public long countUnprocessedByUser(Long userId) {
        LambdaQueryWrapper<HealthRiskWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRiskWarning::getUserId, userId)
                .eq(HealthRiskWarning::getStatus, 0);
        // 使用 MyBatis-Plus 提供的 count 方法
        return mapper.selectCount(wrapper);
    }

    @Override
    public HealthRiskWarning addWarning(HealthWarningAddDTO addDTO) {
        if (addDTO == null || addDTO.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        HealthRiskWarning warning = new HealthRiskWarning();
        warning.setUserId(addDTO.getUserId());
        warning.setRiskType(addDTO.getRiskType());
        warning.setRuleId(addDTO.getRuleId());
        warning.setTriggerData(addDTO.getTriggerData());
        warning.setInterventionContent(addDTO.getInterventionContent());
        warning.setStatus(addDTO.getStatus() == null ? 0 : addDTO.getStatus());
        warning.setTriggerTime(addDTO.getTriggerTime() == null ? LocalDateTime.now() : addDTO.getTriggerTime());
        warning.setCreateTime(LocalDateTime.now());
        warning.setIsDelete(0);
        mapper.insert(warning);
        return warning;
    }

    @Override
    public HealthRiskWarning updateWarning(HealthWarningUpdateDTO updateDTO) {
        if (updateDTO == null || updateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "预警ID不能为空");
        }
        HealthRiskWarning existing = mapper.selectById(updateDTO.getId());
        if (existing == null || Integer.valueOf(1).equals(existing.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "预警不存在");
        }
        HealthRiskWarning warning = new HealthRiskWarning();
        warning.setId(updateDTO.getId());
        warning.setUserId(updateDTO.getUserId());
        warning.setRiskType(updateDTO.getRiskType());
        warning.setRuleId(updateDTO.getRuleId());
        warning.setTriggerData(updateDTO.getTriggerData());
        warning.setInterventionContent(updateDTO.getInterventionContent());
        if (updateDTO.getStatus() != null) {
            warning.setStatus(updateDTO.getStatus());
            warning.setProcessTime(updateDTO.getStatus() == 1 ? LocalDateTime.now() : null);
        }
        warning.setTriggerTime(updateDTO.getTriggerTime());
        mapper.updateById(warning);
        return mapper.selectById(updateDTO.getId());
    }

    @Override
    public boolean deleteWarning(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "预警ID不能为空");
        }
        HealthRiskWarning warning = new HealthRiskWarning();
        warning.setId(id);
        warning.setIsDelete(1);
        return mapper.updateById(warning) > 0;
    }

    @Override
    public IPage<HealthRiskWarning> pageQueryWarnings(HealthWarningPageQueryDTO queryDTO) {
        int pageNum = (queryDTO == null || queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) ? 1 : queryDTO.getPageNum();
        int pageSize = (queryDTO == null || queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) ? 10 : queryDTO.getPageSize();
        Page<HealthRiskWarning> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HealthRiskWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRiskWarning::getIsDelete, 0)
                .eq(queryDTO != null && queryDTO.getUserId() != null, HealthRiskWarning::getUserId, queryDTO.getUserId())
                .eq(queryDTO != null && queryDTO.getStatus() != null, HealthRiskWarning::getStatus, queryDTO.getStatus())
                .like(queryDTO != null && queryDTO.getRiskType() != null && !queryDTO.getRiskType().trim().isEmpty(),
                        HealthRiskWarning::getRiskType, queryDTO.getRiskType().trim())
                .orderByDesc(HealthRiskWarning::getTriggerTime);
        return mapper.selectPage(page, wrapper);
    }
}