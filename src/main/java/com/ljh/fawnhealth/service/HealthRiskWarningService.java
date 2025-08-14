package com.ljh.fawnhealth.service;

import com.ljh.fawnhealth.model.entity.HealthRiskWarning;

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
}