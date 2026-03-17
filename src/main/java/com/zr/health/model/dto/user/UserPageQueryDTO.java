package com.zr.health.model.dto.user;

import lombok.Data;

@Data
public class UserPageQueryDTO {
    /**
     * 页码，默认第1页
     */
    private Long pageNum = 1L;

    /**
     * 每页条数，默认10条
     */
    private Long pageSize = 10L;

    /**
     * 用户ID（精确查询）
     */
    private Long userId;

    /**
     * 用户邮箱（模糊查询，支持部分邮箱匹配）
     */
    private String email;

    /**
     * 性别（精确查询：0未知，1男，2女）
     */
    private Integer gender;

    /**
     * 是否VIP（精确查询：0否，1是）
     */
    private Integer isVip;

    /**
     * 账号状态（精确查询：0禁用，1正常）
     */
    private Integer status;
}