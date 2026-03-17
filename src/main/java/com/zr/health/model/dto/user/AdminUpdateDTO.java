package com.zr.health.model.dto.user;

import lombok.Data;

import java.util.Date;

/**
 * 管理员修改个人信息DTO
 */
@Data
public class AdminUpdateDTO {
    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 管理员登录账号（用户名）
     */
    private String username;

    /**
     * 旧密码
     */
    private String oldPassWord;

    /**
     * 新密码
     */
    private String newPassWord;
}