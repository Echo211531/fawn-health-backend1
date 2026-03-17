package com.zr.health.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.dto.food.DietRecordAddDTO;
import com.zr.health.model.dto.food.DietRecordUpdateDTO;
import com.zr.health.model.entity.DietRecords;
import com.zr.health.model.vo.food.DietRecordDetailVO;
import com.zr.health.model.vo.food.DietRecordSimpleVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 饮食记录服务接口
 * 提供对饮食记录主表（diet_records）的业务操作方法定义
 */
public interface DietRecordsService extends IService<DietRecords> {

    /**
     * 添加饮食记录及其食物项
     *
     * @param dto 添加饮食记录的数据传输对象，包含主记录信息和多个食物项
     * @return 返回本次饮食记录的总热量（单位：千卡）
     */
    BigDecimal addDietRecord(DietRecordAddDTO dto);

    /**
     * 获取某个用户在指定日期的饮食记录（如果未指定日期，默认取当天）
     *
     * @param userId 用户ID
     * @param date 查询的日期（可为空）
     * @return 返回该用户该日的饮食记录列表（简要信息）
     */
    List<DietRecordSimpleVO> getTodayRecords(Long userId, Date date);

    /**
     * 获取用户最近若干天的饮食记录（按天汇总）
     *
     * @param userId 用户ID
     * @param days 要查询的历史天数范围
     * @return 返回该用户在最近 days 天内的饮食记录（简要信息）
     */
    List<DietRecordSimpleVO> getHistoryRecords(Long userId, Integer days);

    /**
     * 获取指定饮食记录的详细信息（包含多个食物项）
     *
     * @param id 饮食记录主键ID
     * @return 返回饮食记录详情视图对象
     */
    DietRecordDetailVO getDietRecordDetail(Long id);

    /**
     * 逻辑删除指定饮食记录（仅标记为删除）
     *
     * @param id 饮食记录主键ID
     * @return 删除是否成功
     */
    boolean deleteDietRecord(Long id);

    /**
     * 修改饮食记录及其对应的食物项（先删除旧项，再插入新项）
     *
     * @param updateDTO 修改数据传输对象
     * @return 修改是否成功
     */
    boolean updateDietRecord(DietRecordUpdateDTO updateDTO);

    /**
     * 查询用户当天摄入的总热量
     *
     * @param userId 用户ID
     * @return 返回用户当天摄入的总热量
     */
    BigDecimal getTodayTotalCalories(Long userId);
}
