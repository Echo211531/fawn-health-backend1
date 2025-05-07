package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.Coupons;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


/**
* @author 27105
* @description 针对表【coupons(优惠券表)】的数据库操作Mapper
* @createDate 2025-05-02 23:02:45
* @Entity com.ljh.domain.Coupons
*/
public interface CouponsMapper extends BaseMapper<Coupons> {

    @Update("UPDATE coupons SET issue_num = issue_num + 1 WHERE id = #{couponsId} AND issue_num < total_num")
    int incrIssueNum(@Param("couponsId") Long couponsId);
}




