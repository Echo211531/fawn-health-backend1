package com.zr.health.controller;

import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.model.dto.food.DietRecordAddDTO;
import com.zr.health.model.dto.food.DietRecordAddResponse;
import com.zr.health.model.dto.food.DietRecordUpdateDTO;
import com.zr.health.model.vo.food.DietRecordDetailVO;
import com.zr.health.model.vo.food.DietRecordSimpleVO;
import com.zr.health.service.DietRecordsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 饮食记录模块
 * 提供饮食记录的增删改查等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/foodDiary")
public class  FoodDiaryController {
    @Resource
    private DietRecordsService dietRecordsService;

    /**
     * 添加饮食记录（包含多个食物项）
     *
     * @param addDTO 前端传入的饮食记录添加数据传输对象
     * @return 返回包含操作结果和总热量的响应体
     */
    @PostMapping("/addDietRecord")
    public BaseResponse<DietRecordAddResponse> addDietRecord(@RequestBody DietRecordAddDTO addDTO) {
        log.info("添加饮食记录: {}", addDTO);

        BigDecimal totalCalories = dietRecordsService.addDietRecord(addDTO);
        // totalCalories为0说明添加失败（可根据需求优化为异常或Optional处理）
        boolean result = totalCalories.compareTo(BigDecimal.ZERO) > 0;

        DietRecordAddResponse response = new DietRecordAddResponse();
        response.setSuccess(result);
        response.setTotalCalories(totalCalories);
        return ResultUtils.success(response);
    }

    /**
     * 获取指定用户某日饮食记录（默认当天）
     *
     * @param userId 用户ID，必填
     * @param date 查询日期，非必填，不传则默认为当天
     * @return 返回指定日期用户的饮食记录简要列表
     */
    @GetMapping("/getTodayRecords")
    public BaseResponse<List<DietRecordSimpleVO>> getTodayRecords(
            @RequestParam Long userId,
            @RequestParam(required = false) Date date) {
        log.info("查询用户 {} 的当日饮食记录，日期: {}", userId, date);
        List<DietRecordSimpleVO> records = dietRecordsService.getTodayRecords(userId, date);
        return ResultUtils.success(records);
    }

    /**
     * 获取指定用户最近若干天的历史饮食记录
     *
     * @param userId 用户ID，必填
     * @param days 查询天数范围，必填
     * @return 返回最近days天内的饮食记录简要列表
     */
    @GetMapping("/getHistoryRecords")
    public BaseResponse<List<DietRecordSimpleVO>> getHistoryRecords(
            @RequestParam Long userId,
            @RequestParam Integer days) {
        log.info("查询用户 {} 的历史饮食记录，最近 {} 天", userId, days);
        List<DietRecordSimpleVO> records = dietRecordsService.getHistoryRecords(userId, days);
        return ResultUtils.success(records);
    }

    /**
     * 根据饮食记录ID获取详细信息
     *
     * @param id 饮食记录ID
     * @return 返回该条饮食记录的详细数据
     */
    @GetMapping("/getDietRecordDetail/{id}")
    public BaseResponse<DietRecordDetailVO> getDietRecordDetail(@PathVariable Long id) {
        log.info("查询饮食记录详情，ID: {}", id);
        DietRecordDetailVO detail = dietRecordsService.getDietRecordDetail(id);
        return ResultUtils.success(detail);
    }

    /**
     * 删除饮食记录（逻辑删除）
     *
     * @param id 饮食记录ID
     * @return 操作结果，true表示删除成功，false表示失败
     */
    @GetMapping("/deleteDietRecord/{id}")
    public BaseResponse<Boolean> deleteDietRecord(@PathVariable Long id) {
        log.info("删除饮食记录，ID: {}", id);
        boolean success = dietRecordsService.deleteDietRecord(id);
        return ResultUtils.success(success);
    }

    /**
     * 修改饮食记录（包含多个食物项）
     *
     * @param updateDTO 前端传入的饮食记录更新数据传输对象
     * @return 操作结果，true表示更新成功，false表示失败
     */
    @PostMapping("/updateDietRecord")
    public BaseResponse<Boolean> updateDietRecord(@RequestBody DietRecordUpdateDTO updateDTO) {
        log.info("修改饮食记录: {}", updateDTO);

        boolean success = dietRecordsService.updateDietRecord(updateDTO);
        return ResultUtils.success(success);
    }

    /**
     * 查询用户当天摄入的总热量
     *
     * @param userId 用户ID
     * @return 返回用户当天摄入的总热量
     */
    @GetMapping("/getTodayTotalCalories")
    public BaseResponse<BigDecimal> getTodayTotalCalories(@RequestParam Long userId) {
        log.info("查询用户 {} 当天摄入的总热量", userId);
        BigDecimal totalCalories = dietRecordsService.getTodayTotalCalories(userId);
        return ResultUtils.success(totalCalories);
    }

}
