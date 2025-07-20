package com.ljh.fawnhealth.model.dto.address;

import lombok.Data;

/**
 * 创建收货地址的请求参数DTO
 * 用于接收前端传递的收货地址创建信息，并进行参数校验
 */
@Data
public class AddressCreateDTO {

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
     * 默认为0（非默认）
     */
    private Integer isDefault = 0;
}