package com.zr.health.config;

import com.zr.health.commen.BaseResponse;
import com.zr.health.exception.ErrorCode;

/**
 * 响应包装工具类
 * 用于统一返回结果封装
 */
public class ResultUtils {

    /**
     * 成功，带数据
     *
     * @param data 返回数据
     * @param <T>  数据类型
     * @return 通用成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, data, "ok");
    }

    /**
     * 成功，无数据
     *
     * @return 通用成功响应
     */
    public static BaseResponse<?> success() {
        return new BaseResponse<>(200, null, "ok");
    }


    /**
     * 失败，直接传错误码和自定义信息
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 通用失败响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }
    /**
     * 失败，返回错误码
     *
     * @param errorCode 错误码对象
     * @param <T> 泛型
     * @return 通用失败响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * 失败，自定义错误信息
     *
     * @param errorCode 错误码对象
     * @param message   自定义错误描述
     * @return 通用失败响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }


}
