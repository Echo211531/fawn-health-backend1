package com.zr.health.model.vo.user;

import lombok.Data;

import java.util.List;

/**
 * 用户性别分布统计VO
 */
@Data
public class UserGenderStatisticsVO {
    /**
     * 性别标签列表（如：["未知", "男", "女"]）
     */
    private List<String> genderLabels;

    /**
     * 对应性别数量列表（如：[10, 200, 150]）
     */
    private List<Integer> genderCounts;

    /**
     * 总用户数
     */
    private Integer totalUserCount;
}