package com.ljh.fawnhealth.model.dto.address;

import lombok.Data;

/**
 * 更新收货地址的请求参数DTO
 * 用于接收前端传递的收货地址更新信息，并进行参数校验
 */
@Data
public class AddressUpdateDTO {

    /**
     * 地址ID
     * 必须提供，用于标识要更新的地址
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人联系电话
     * 采用正则表达式校验手机号格式（11位数字，以13/14/15/16/17/18/19开头）
     */
    private String receiverPhone;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区/县
     */
    private String district;

    /**
     * 详细地址
     * 具体到街道、门牌号等信息
     */
    private String detailAddress;

    /**
     * 邮政编码
     * 非必填项
     */
    private String postalCode;

    /**
     * 是否默认地址
     * 0-非默认地址，1-默认地址
     */
    private Integer isDefault;
}
