package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.DietRecords;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
* @author 27105
* @description 针对表【diet_records(饮食记录主表)】的数据库操作Mapper
* @createDate 2025-05-25 18:10:41
* @Entity com.ljh.domain.DietRecords
*/
public interface DietRecordsMapper extends BaseMapper<DietRecords> {
    List<DietRecords> selectByUserIdAndDate(@Param("userId") Long userId, @Param("recordDate") Date recordDate);
}




