package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.HealthRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 健康规则Mapper接口
 */
public interface HealthRuleMapper extends BaseMapper<HealthRule> {

    /**
     * 查询所有启用的规则，按优先级排序
     */
    List<HealthRule> selectEnabledRulesOrderByPriority();

    /**
     * 根据风险类型查询启用的规则
     */
    List<HealthRule> selectByRiskTypeAndEnabled(@Param("riskType") String riskType);

    /**
     * 根据规则名称模糊查询
     */
    List<HealthRule> selectByNameLike(@Param("name") String name);
}