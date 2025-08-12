package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.entity.HealthAssessment;


/**
* @author 27105
* @description 针对表【health_assessment(用户健康评估每日记录表)】的数据库操作Service
* @createDate 2025-08-09 20:15:30
*/
public interface HealthAssessmentService extends IService<HealthAssessment> {

    /**
     * 执行每日健康评估
     * @param userId 用户ID（必填）
     * @return 包含评估结果的成功响应
     */
    HealthAssessment dailyAssessment(Long userId);

    /**
     * 获取用户最新健康评估结果
     * @param userId 用户ID（必填）
     * @return 最新评估结果或null
     */
    HealthAssessment getLatestByUser(Long userId);
}
