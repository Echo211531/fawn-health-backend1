package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.rule.InterventionPlanPageQueryDTO;
import com.ljh.fawnhealth.model.enums.rule.InterventionPlan;
import com.ljh.fawnhealth.service.InterventionPlanService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/intervention-plans")
public class InterventionPlanController {

    @Resource
    private InterventionPlanService interventionPlanService;

    /**
     * 添加干预方案
     */
    @PostMapping("/add")
    public BaseResponse<InterventionPlan> add(@RequestBody InterventionPlan plan) {
        return ResultUtils.success(interventionPlanService.create(plan));
    }

    /**
     * 更新干预方案
     */
    @PostMapping("/update")
    public BaseResponse<InterventionPlan> updatePlan(@RequestBody InterventionPlan plan) {
        return ResultUtils.success(interventionPlanService.update(plan));
    }

    /**
     * 删除干预方案（逻辑删除）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePlan(@RequestParam Long id) {
        return ResultUtils.success(interventionPlanService.delete(id));
    }

    /**
     * 获取干预方案详情
     */
    @GetMapping("/getDetail")
    public BaseResponse<InterventionPlan> getDetail(@RequestParam Long id) {
        return ResultUtils.success(interventionPlanService.getById(id));
    }

    /**
     * 条件列表查询（不分页）
     */
    @GetMapping("/list")
    public BaseResponse<List<InterventionPlan>> list(@RequestParam(required = false) String riskType,
            @RequestParam(required = false) Boolean enabled) {
        return ResultUtils.success(interventionPlanService.list(riskType, enabled));
    }

    /**
     * 分页查询，支持按riskType / enabled 过滤
     * 若需根据预警ID过滤，请在DTO新增字段 warningId 后在服务层拼接对应条件
     */
    @PostMapping("/pageQuery")
    public BaseResponse<IPage<InterventionPlan>> pageQuery(@RequestBody InterventionPlanPageQueryDTO queryDTO) {
        return ResultUtils.success(interventionPlanService.pageQuery(queryDTO));
    }

    /**
     * 启用/禁用
     */
    @PostMapping("/toggle")
    public BaseResponse<Boolean> toggle(@RequestParam Long id, @RequestParam boolean enabled) {
        return ResultUtils.success(interventionPlanService.toggle(id, enabled));
    }
}