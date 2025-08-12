package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.UserWeightHistory;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
* @author 27105
* @description 针对表【user_weight_history(用户体重历史记录)】的数据库操作Mapper
* @createDate 2025-08-09 20:58:27
* @Entity com.ljh.domain.UserWeightHistory
*/
public interface UserWeightHistoryMapper extends BaseMapper<UserWeightHistory> {

    List<UserWeightHistory> selectByUserIdAndDateRange(Long userId, Date startDate, Date endDate);
}




