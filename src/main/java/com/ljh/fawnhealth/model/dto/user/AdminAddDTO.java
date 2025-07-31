package com.ljh.fawnhealth.model.dto.user;

import lombok.Data;

@Data
public class AdminAddDTO {

    /**
     * 管理员登录账号（用户名）
     * 非空且长度限制（1-50字符）
     */
    private String username;

    /**
     * 性别：0未知，1男，2女
     * 必须为有效枚举值
     */
    private Integer gender;

    /**
     * 邮箱（唯一）
     * 格式校验
     */
    private String email;
}