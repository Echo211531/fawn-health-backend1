package com.ljh.fawnhealth.model.vo.user;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户登录统计VO
 */
@Data
public class UserLoginStatisticsVO {

    /**
     * 总登录用户数
     */
    private long userTotal;

    /**
     * 今日登录用户数
     */
    private long todayLoginUsers;

    /**
     * 昨日登录用户数
     */
    private long yesterdayLoginUsers;

    /**
     * 本月登录用户数
     */
    private long monthLoginUsers;

    /**
     * 日环比增长率(%)
     */
    private BigDecimal dayOnDayRate;

    /**
     * 统计生成时间
     */
    private LocalDateTime statisticTime;
}
