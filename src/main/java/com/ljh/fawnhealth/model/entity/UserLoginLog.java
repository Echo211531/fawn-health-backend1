package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户登录日志表
 * @TableName user_login_log
 */
@TableName(value ="user_login_log")
@Data
public class UserLoginLog implements Serializable {
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联user表的id
     */
    private Long userId;

    /**
     * 登录时间
     */
    private Date loginTime;

    /**
     * 登录状态：1-成功，0-失败
     */
    private Integer loginStatus;

    /**
     * 登录失败原因（如密码错误、账号锁定等）
     */
    private String failReason;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}