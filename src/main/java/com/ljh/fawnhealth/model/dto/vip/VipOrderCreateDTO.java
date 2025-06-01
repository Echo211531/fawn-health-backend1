package com.ljh.fawnhealth.model.dto.vip;

import lombok.Data;

/**
 * VIP订单创建请求数据传输对象
 * 用于接收前端提交的VIP充值订单信息
 */
@Data
public class VipOrderCreateDTO {

    /**
     * 用户ID
     * */
    private Long userId;

    /**
     * VIP类型
     * 取值：1-月卡，2-季卡，3-年卡
     */
    private Integer vipType;

    /**
     * 支付方式
     * 取值：1-微信支付，2-支付宝，3-苹果支付
     */
    private Integer paymentMethod;

    /**
     * 订单备注
     */
    private String remark;
}