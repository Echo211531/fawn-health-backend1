package com.zr.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zr.health.model.dto.healthWarning.HealthWarningAddDTO;
import com.zr.health.model.dto.healthWarning.HealthWarningPageQueryDTO;
import com.zr.health.model.dto.healthWarning.HealthWarningUpdateDTO;
import com.zr.health.model.entity.HealthRiskWarning;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthRiskWarningService {

    void save(HealthRiskWarning warning);

    List<HealthRiskWarning> listByUser(Long userId);

    List<HealthRiskWarning> listByUserAndTimeRange(Long userId, LocalDateTime start, LocalDateTime end);

    List<HealthRiskWarning> listUnprocessed();

    /**
     * 标记指定预警为已处理，并设置处理时间
     * 
     * @return true 表示成功更新；false 表示未更新（可能已处理或不存在）
     */
    boolean markProcessed(Long id);

    /**
     * 获取用户最近的未处理预警记录（按触发时间倒序，限制条数）
     */
    List<HealthRiskWarning> getLatestUnprocessedByUser(Long userId, int limit);

    /**
     * 统计用户未处理预警数量
     */
    long countUnprocessedByUser(Long userId);

    /**
     * 管理端新增预警
     */
    HealthRiskWarning addWarning(HealthWarningAddDTO addDTO);

    /**
     * 管理端更新预警
     */
    HealthRiskWarning updateWarning(HealthWarningUpdateDTO updateDTO);

    /**
     * 管理端删除预警（逻辑删除）
     */
    boolean deleteWarning(Long id);

    /**
     * 管理端分页查询预警
     */
    IPage<HealthRiskWarning> pageQueryWarnings(HealthWarningPageQueryDTO queryDTO);
}