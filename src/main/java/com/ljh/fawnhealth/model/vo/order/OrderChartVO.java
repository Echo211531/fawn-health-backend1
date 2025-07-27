package com.ljh.fawnhealth.model.vo.order;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单统计结果VO（适配前端图表）
 */
@Data
public class OrderChartVO {
    // 金额数据：当前周期、对比周期（如本月 vs 上月）
    private List<BigDecimal> currentAmounts; // 如：本月每天金额
    private List<BigDecimal> compareAmounts; // 如：上月每天金额

    // 数量数据：当前周期、对比周期
    private List<Long> currentCounts; // 如：本月每天订单数
    private List<Long> compareCounts; // 如：上月每天订单数

    // 时间轴（X轴标签）
    private List<String> timeLabels; // 如：1号、2号...或一月、二月...
}