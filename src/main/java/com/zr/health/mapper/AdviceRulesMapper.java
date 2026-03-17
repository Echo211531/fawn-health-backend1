package com.zr.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zr.health.model.entity.AdviceRules;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 27105
* @description 针对表【advice_rules(健康建议规则表)】的数据库操作Mapper
* @createDate 2025-08-09 20:18:00
* @Entity com.ljh.domain.AdviceRules
*/
public interface AdviceRulesMapper extends BaseMapper<AdviceRules> {

    @Select("SELECT * FROM advice_rules ORDER BY priority ASC")
    List<AdviceRules> selectAllOrderByPriority();

}




