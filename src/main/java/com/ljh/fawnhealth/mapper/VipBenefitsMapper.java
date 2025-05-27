package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.VipBenefits;
import com.ljh.fawnhealth.model.vo.vip.VipBenefitsVO;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
* @author 27105
* @description 针对表【vip_benefits(VIP权益表)】的数据库操作Mapper
* @createDate 2025-05-25 22:13:35
* @Entity com.ljh.domain.VipBenefits
*/
public interface VipBenefitsMapper extends BaseMapper<VipBenefits> {

    /**
     * 查询所有启用的会员权益
     */
    @Select("SELECT vip_type, price, benefit_name, description, value " +
            "FROM vip_benefits " +
            "WHERE status = 1 AND is_delete = 0 " +
            "ORDER BY sort_order ASC")
    List<VipBenefitsVO> selectAllEnabledBenefits();

    BigDecimal selectVipPriceByType(Integer vipType);

    /**
     * 根据会员类型查询对应权益
     */
    @Select("SELECT vip_type, price, benefit_name, description, value " +
            "FROM vip_benefits " +
            "WHERE status = 1 AND is_delete = 0 " +
            "AND vip_type = #{vipType} " +
            "ORDER BY sort_order ASC")
    List<VipBenefitsVO> selectBenefitsByVipType(Integer vipType);
}




