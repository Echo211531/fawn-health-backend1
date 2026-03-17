package com.zr.health.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.entity.HealthReport;

import java.util.List;

/**
* @author 27105
* @description 针对表【health_report(用户健康分析报告表)】的数据库操作Service
* @createDate 2025-08-09 20:17:54
*/
public interface HealthReportService extends IService<HealthReport> {

    /**
     * 生成健康周报
     * @param userId 用户ID（必填）
     * @return 包含周报数据的成功响应
     */
    HealthReport generateWeeklyReport(Long userId);

    /**
     * 分页查询健康报告
     * @param userId 用户ID（必填）
     * @param reportType 报告类型（可选：1-周报 2-月报）
     * @param limit 返回条数（默认5，最大100）
     * @return 分页报告列表
     */
    List<HealthReport> getReports(Long userId, Integer reportType, Integer limit);
}
