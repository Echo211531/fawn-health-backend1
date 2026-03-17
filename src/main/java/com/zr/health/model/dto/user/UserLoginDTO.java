package com.zr.health.model.dto.user;

import lombok.Data;

/**
 * 用户登录数据传输对象（DTO），用于封装用户登录时所需传递的信息。
 */
@Data
public class UserLoginDTO {
    /**
     * 邮箱。
     */
    private String email;

    /**
     * 验证码。
     */
    private String code;
}