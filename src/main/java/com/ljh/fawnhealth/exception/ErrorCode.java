package com.ljh.fawnhealth.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "ok"),

    // 用户表相关错误码 40000 - 40100
    PARAMS_ERROR(40000, "请求参数错误"),
    USERNAME_EXISTS(40001, "用户名已存在"),
    PHONE_EXISTS(40002, "手机号已注册"),
    EMAIL_EXISTS(40003, "邮箱已注册"),
    PHONE_VERIFICATION_FAILED(40004, "手机号验证失败"),
    EMAIL_VERIFICATION_FAILED(40005, "邮箱验证失败"),
    EMAIL_CODE(40005, "验证码错误或已过期"),
    PASSWORD_TOO_WEAK(40006, "密码强度不足"),
    ACCOUNT_DISABLED(40007, "账号已禁用"),
    UPLOAD_FAILED(40008,"文件上传错误"),


    // 登录相关错误码 40100 - 40200
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    INVALID_CREDENTIALS(40102, "用户名或密码错误"),

    // 健康数据相关错误码 40200 - 40300
    WEIGHT_RECORD_NOT_FOUND(40200, "体重记录不存在"),
    DIET_RECORD_NOT_FOUND(40201, "饮食记录不存在"),
    EXERCISE_RECORD_NOT_FOUND(40202, "运动记录不存在"),
    INVALID_FOOD_ID(40203, "无效的食物ID"),
    INVALID_EXERCISE_ID(40204, "无效的运动项目ID"),

    // 食物与运动基础数据相关错误码 40300 - 40400
    FOOD_NOT_FOUND(40300, "食物不存在"),
    FOOD_CATEGORY_NOT_FOUND(40301, "食物分类不存在"),
    EXERCISE_NOT_FOUND(40302, "运动项目不存在"),
    EXERCISE_CATEGORY_NOT_FOUND(40303, "运动分类不存在"),

    // 健康方案与社区相关错误码 40400 - 40500
    HEALTH_PLAN_NOT_FOUND(40400, "健康方案不存在"),
    DAILY_RECIPE_NOT_FOUND(40401, "每日食谱不存在"),
    COMMUNITY_POST_NOT_FOUND(40402, "社区帖子不存在"),
    COMMENT_NOT_FOUND(40403, "评论不存在"),
    POST_TYPE_ERROR(40404, "帖子类型错误"),

    // 系统与会员相关错误码 40500 - 40600
    VIP_ORDER_NOT_FOUND(40500, "会员订单不存在"),
    SYSTEM_MESSAGE_NOT_FOUND(40501, "系统消息不存在"),
    COUPON_NOT_FOUND(40502, "优惠券不存在"),
    COUPON_SCOPE_NOT_FOUND(40503, "优惠券使用范围不存在"),
    EXCHANGE_CODE_NOT_FOUND(40504, "兑换码不存在"),
    USER_COUPON_NOT_FOUND(40505, "用户优惠券不存在"),
    COUPON_USAGE_LOG_NOT_FOUND(40506, "优惠券使用记录不存在"),
    COUPON_EXPIRED(40507, "优惠券已过期"),
    COUPON_ALREADY_USED(40508, "优惠券已使用"),
    INVALID_VIP_TYPE(40509, "无效的会员类型"),
    INVALID_PAYMENT_METHOD(40510, "无效的支付方式"),
    ORDER_STATUS_ERROR(40511, "订单状态错误"),
    COUPON_STOCK(40512,"优惠券库存不足"),
    COUPON_BEGIN_END(40513,"优惠券发放已经结束或尚未开始"),
    COUPON_OVER_LIMIT(40514, "优惠券超出领取数量"),
    EXCHANGE_CODE_OVERDUE(40515,"兑换码已经过期"),
    EXCHANGE_CODE_USE(40515,"兑换码已经被兑换过了"),






    // 商品模块相关错误码 40600 - 40700
    PRODUCT_CATEGORY_NOT_FOUND(40600, "商品分类不存在"),
    PRODUCT_NOT_FOUND(40601, "商品不存在"),
    PRODUCT_SKU_NOT_FOUND(40602, "商品SKU不存在"),
    PRODUCT_REVIEW_NOT_FOUND(40603, "商品评价不存在"),
    PRODUCT_FAVORITE_NOT_FOUND(40604, "商品收藏不存在"),
    PRODUCT_OUT_OF_STOCK(40605, "商品无库存"),

    // 系统配置相关错误码 40700 - 40800
    SYSTEM_CONFIG_NOT_FOUND(40700, "系统配置不存在"),
    OPERATION_LOG_NOT_FOUND(40701, "操作日志不存在"),


    // 通用错误码 50000 - 50100
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    FORBIDDEN_ERROR(50002, "禁止访问"),
    DATA_INTEGRITY_ERROR(50003, "数据完整性错误"),
    REQUEST_ARE_FREQUENT(50004,"请求频繁"),
    SERVICE_UNAVAILABLE(50005, "服务不可用");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;


    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }


}