package com.ljh.fawnhealth.model.vo.user;

import lombok.Data;
import java.util.Date;

@Data
public class UserVO {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证状态
     */
    private Integer emailVerified;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 性别:0未知,1男,2女
     */
    private Integer gender;

    /**
     * 角色
     */
    private String role;

    /**
     * 是否VIP:0否,1是
     */
    private Integer isVip;

    /**
     * VIP过期时间
     */
    private Date vipExpireTime;

    /**
     * VIP等级
     */
    private Integer vipLevel;

    /**
     * 账号状态:0禁用,1正常
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
