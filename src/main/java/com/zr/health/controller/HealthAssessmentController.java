package com.zr.health.controller;

import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.model.entity.HealthAssessment;
import com.zr.health.model.entity.HealthReport;
import com.zr.health.service.HealthAssessmentService;
import com.zr.health.service.HealthReportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 健康评估管
 * 提供健康评估、报告生成与查询等核心功能
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthAssessmentController {

    @Resource
    private HealthAssessmentService healthAssessmentService;
    @Resource
    private HealthReportService reportService;

    /**
     * 执行每日健康评估
     * @param userId 用户ID（必填）
     * @return 包含评估结果的成功响应
     */
    @PostMapping("/daily")
    public BaseResponse<HealthAssessment> doDailyAssessment(
            @RequestParam Long userId) {
        HealthAssessment assessment = healthAssessmentService.dailyAssessment(userId);
        return ResultUtils.success(assessment);
    }

    /**
     * 获取用户最新健康评估结果
     * @param userId 用户ID（必填）
     * @return 最新评估结果或null
     */
    @GetMapping("/latest")
    public BaseResponse<HealthAssessment> getLatestAssessment(@RequestParam Long userId) {
        HealthAssessment assessment = healthAssessmentService.getLatestByUser(userId);
        return ResultUtils.success(assessment);
    }

    /**
     * 生成健康周报
     * @param userId 用户ID（必填）
     * @return 包含周报数据的成功响应
     */
    @PostMapping("/reports/weekly")
    public BaseResponse<HealthReport> generateWeeklyReport(
            @RequestParam Long userId) {
        HealthReport report = reportService.generateWeeklyReport(userId);
        return ResultUtils.success(report);
    }

    /**
     * 分页查询健康报告
     * @param userId 用户ID（必填）
     * @param reportType 报告类型（可选：1-周报 2-月报）
     * @param limit 返回条数（默认5，最大100）
     * @return 分页报告列表
     */
    @GetMapping("/reports")
    public BaseResponse<List<HealthReport>> getReports(
            @RequestParam Long userId,
            @RequestParam(required = false)  Integer reportType,
            @RequestParam(defaultValue = "5") Integer limit) {
        List<HealthReport> reports = reportService.getReports(userId, reportType, limit);
        return ResultUtils.success(reports);
    }
}