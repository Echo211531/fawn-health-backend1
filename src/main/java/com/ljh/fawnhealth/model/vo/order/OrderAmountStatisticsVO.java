package com.ljh.fawnhealth.model.vo.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单金额统计VO
 */
@Data
public class OrderAmountStatisticsVO {

    /**
     * 全部订单总金额
     */
    private BigDecimal OrderTotalAmount;


    /**
     * 今日订单总金额
     */
    private BigDecimal todayOrderAmount;

    /**
     * 昨日订单总金额
     */
    private BigDecimal yesterdayOrderAmount;

    /**
     * 本月订单总金额
     */
    private BigDecimal monthOrderAmount;

    /**
     * 日环比增长率(%)
     */
    private BigDecimal dayOnDayRate;

    /**
     * 统计生成时间
     */
    private LocalDateTime statisticTime;
}