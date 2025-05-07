package com.ljh.fawnhealth.model.vo.coupons;


import com.ljh.fawnhealth.model.enums.coupons.DiscountType;
import com.ljh.fawnhealth.model.enums.coupons.ObtainType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券详细数据
 */
@Data
public class CouponsDetailVO {

    /**
     * 优惠券id，新增不需要添加，更新必填
     */
    private Long id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券使用范围，key范围类型(0-没有限制，1-限定分类，2-限定课程)，值是范围ID
     */
    private List<CouponsScopeVO> scopes;

    /**
     * 优惠券类型，1：每满减，2：折扣，3：无门槛，4：普通满减
     */
    private DiscountType discountType;

    /**
     * 折扣门槛，0代表无门槛
     */
    private Integer thresholdAmount;

    /**
     * 折扣值，满减填抵扣金额；打折填折扣值：80标示打8折
     */
    private Integer discountValue;

    /**
     * 最大优惠金额
     */
    private Integer maxDiscountAmount;

    /**
     * 发放开始时间
     */
    private LocalDateTime issueBeginTime;

    /**
     * 发放结束时间
     */
    private LocalDateTime issueEndTime;

    /**
     * 有效天数
     */
    private Integer termDays;

    /**
     * 使用有效期开始时间
     */
    private LocalDateTime termBeginTime;

    /**
     * 使用有效期结束时间
     */
    private LocalDateTime termEndTime;

    /**
     * 优惠券总量，如果为0代表无上限
     */
    private Integer totalNum;

    /**
     * 每人领取的上限
     */
    private Integer userLimit;

    /**
     * 获取方式1：手动领取，2：指定发放（通过兑换码兑换）
     */
    private ObtainType obtainWay;
}
