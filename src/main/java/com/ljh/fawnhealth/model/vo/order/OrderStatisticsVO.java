package com.ljh.fawnhealth.model.vo.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单统计VO
 */
@Data
public class OrderStatisticsVO {
    /**
     * 今日订单数量
     */
    private long todayOrderCount;

    /**
     * 昨日订单数量
     */
    private long yesterdayOrderCount;

    /**
     * 本月订单数量
     */
    private long monthOrderCount;

    /**
     * 日环比增长率(%)
     */
    private BigDecimal dayOnDayRate;

    /**
     * 统计生成时间
     */
    private LocalDateTime statisticTime;
}