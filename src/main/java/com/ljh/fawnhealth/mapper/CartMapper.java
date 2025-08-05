package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.Cart;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
* @author 27105
* @description 针对表【cart(购物车表)】的数据库操作Mapper
* @createDate 2025-07-14 23:01:15
* @Entity com.ljh.domain.Cart
*/
public interface CartMapper extends BaseMapper<Cart> {

    Cart selectByUserIdAndProductId(Long userId, Long productId);

    /**
     * 根据用户ID查询购物车记录
     * @param userId 用户ID
     * @return 购物车记录列表
     */
    @Select("SELECT id, user_id, product_id, quantity, selected, specs, create_time, update_time " +
            "FROM cart " +
            "WHERE user_id = #{userId} " +
            "AND is_delete = 0 " +
            "ORDER BY update_time DESC")
    List<Cart> selectByUserId(Long userId);
}




