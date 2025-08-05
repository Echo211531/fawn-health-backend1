package com.ljh.fawnhealth.model.dto.order;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 订单中课程及优惠券信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCouponDTO {

    /**
     * 用户优惠券id
     */
    private List<Long> userCouponIds;

    /**
     * 订单中的商品列表
     */
    private List<OrderProductDTO> courseList;
}
