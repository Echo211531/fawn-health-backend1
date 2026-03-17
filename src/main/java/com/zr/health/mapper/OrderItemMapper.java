package com.zr.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zr.health.model.entity.OrderItem;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

/**
* @author 27105
* @description 针对表【order_item(订单商品明细表)】的数据库操作Mapper
* @createDate 2025-07-14 23:00:48
* @Entity com.ljh.domain.OrderItem
*/
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    // 直接通过参数传递条件，避免使用 ew 变量
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM order_item WHERE product_id = #{productId} AND is_delete = #{isDelete}")
    Integer sumQuantity(@Param("productId") Long productId, @Param("isDelete") Integer isDelete);

}




