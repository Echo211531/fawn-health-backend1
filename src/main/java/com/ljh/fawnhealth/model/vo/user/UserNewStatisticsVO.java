package com.ljh.fawnhealth.model.vo.user;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户新增统计VO
 */
@Data
public class UserNewStatisticsVO {

    /**
     * 总用户数
     */
    private long userTotal;

    /**
     * 今日新增用户数
     */
    private long todayNewUsers;

    /**
     * 昨日新增用户数
     */
    private long yesterdayNewUsers;

    /**
     * 本月新增用户数
     */
    private long monthNewUsers;

    /**
     * 日环比增长率(%)
     */
    private BigDecimal dayOnDayRate;

    /**
     * 统计生成时间
     */
    private LocalDateTime statisticTime;
}