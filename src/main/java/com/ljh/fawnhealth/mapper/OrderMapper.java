package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.Order;
import com.ljh.fawnhealth.model.entity.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;


/**
* @author 27105
* @description 针对表【order(订单表)】的数据库操作Mapper
* @createDate 2025-07-14 23:00:39
* @Entity com.ljh.domain.Order
*/
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 统计商品关联的订单数量
     * @param productId 商品ID
     * @return 关联订单数量
     */
    int countByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM `order` WHERE order_no = #{orderNo}")
    Order selectByOrderNo(String orderNo);

    /**
     * 统计指定条件的订单总金额
     * @param queryWrapper 查询条件
     * @return 订单总金额
     */
    BigDecimal sumOrderAmount(@Param("ew") Wrapper<Order> queryWrapper);

    BigDecimal sumTotalAmount(@Param("ew") Wrapper<Order> queryWrapper);

}




