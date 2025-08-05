package com.ljh.fawnhealth.model.dto.coupons;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ljh.fawnhealth.model.enums.coupons.DiscountType;
import com.ljh.fawnhealth.model.enums.coupons.ObtainType;
import lombok.Data;
import java.util.List;

/**
 * 优惠券表单数据
 */
@Data
public class CouponsFormDTO {

    /**
     * 优惠券id
     */
    private Long id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 是否添限定使用范围，true：限定了，false：没限定
     */
    @TableField("`specific`")
    private Boolean specific;

    /**
     * 优惠券使用范围
     */
    private List<Long> scopes;

    /**
     * 优惠券类型，1：每满减，2：折扣，3：无门槛，4：普通满减
     */
    private Integer discountType;

    /**
     * 折扣门槛，0代表无门槛
     */
    private Integer thresholdAmount;

    /**
     * 折扣值，满减填抵扣金额；打折填折扣值：80表示打8折
     */
    private Integer discountValue;

    /**
     * 最大优惠金额
     */
    private Integer maxDiscountAmount;

    /**
     * 优惠券总量
     */
    private Integer totalNum;

    /**
     * 每人领取的上限
     */
    private Integer userLimit;

    /**
     * 获取方式  1：手动领取，2：指定发放（通过兑换码兑换）
     */
    private Integer obtainWay;

}