package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.order.*;
import com.ljh.fawnhealth.model.vo.order.*;
import com.ljh.fawnhealth.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单模块
 * 提供订单创建、删除等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 创建成功的订单信息
     */
    @PostMapping("/createOrder")
    public BaseResponse<OrderVO> createOrder(@RequestBody OrderCreateDTO orderCreateDTO) {
        log.info("创建订单: {}", orderCreateDTO);
        OrderVO orderVO = orderService.createOrder(orderCreateDTO);
        return ResultUtils.success(orderVO);
    }

    /**
     * 根据用户ID和订单状态查询订单列表（GET请求，无分页）
     *
     * @param userId 用户ID（必填）
     * @param status 订单状态（可选，null时查所有状态）
     * @return 订单列表
     */
    @GetMapping("/list")
    public BaseResponse<List<OrderVO>> getOrderList(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer status) {  // status可选
        log.info("查询订单列表，用户ID：{}，订单状态：{}", userId, status);
        List<OrderVO> orderVOList = orderService.getOrderListByUserIdAndStatus(userId, status);
        return ResultUtils.success(orderVOList);
    }

    /**
     * 根据订单ID查询订单详细信息
     *
     * @param orderId 订单ID（必填）
     * @return 订单详细信息（包含商品、地址等）
     */
    @GetMapping("/detail")
    public BaseResponse<OrderVO> getOrderDetail(@RequestParam Long orderId) {
        log.info("查询订单详情，订单ID：{}", orderId);
        OrderVO orderVO = orderService.getOrderDetailById(orderId);
        return ResultUtils.success(orderVO);
    }

    /**
     * 订单统计接口(总的订单数量)
     * 获取今日、昨日订单数量及日环比增长率
     *
     * @return 订单统计结果VO
     */
    @GetMapping("/statistics/orderCounts")
    public BaseResponse<OrderStatisticsVO> getOrderStatistics() {
        OrderStatisticsVO statisticsVO = orderService.getOrderStatistics();
        return ResultUtils.success(statisticsVO);
    }

    /**
     * 订单金额统计接口
     * 获取今日、昨日订单总金额及日环比增长率
     *
     * @return 订单金额统计结果VO
     */
    @GetMapping("/statistics/orderAmounts")
    public BaseResponse<OrderAmountStatisticsVO> getOrderAmountStatistics() {
        OrderAmountStatisticsVO statisticsVO = orderService.getOrderAmountStatistics();
        return ResultUtils.success(statisticsVO);
    }

    /**
     * 多维度订单统计接口
     *
     * @param dimension 统计维度：30天/周/月/年
     * @return 图表渲染数据
     */
    @GetMapping("/statistics/chart")
    public BaseResponse<OrderChartVO> getOrderChart(
            @RequestParam(defaultValue = "30天") String dimension) {
        OrderChartVO chartVO = orderService.statisticOrder(dimension);
        return ResultUtils.success(chartVO);
    }

    /**
     * 分页查询全部订单信息
     * @param queryDTO 分页查询参数（页码、每页条数、状态筛选等）
     * @return 分页订单VO列表
     */
    @PostMapping("/page")
    public BaseResponse<IPage<OrderVO>> queryAllOrdersByPage(@RequestBody OrderPageQueryDTO queryDTO) {
        log.info("OrderPageQueryTempDTO: {}", queryDTO.toString());
        // 校验分页参数（避免页码或条数为负数）
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10); // 限制最大每页100条
        }

        IPage<OrderVO> orderPage = orderService.queryAllOrdersByPage(queryDTO);
        return ResultUtils.success(orderPage);
    }

    /**
     * 修改订单状态接口
     * @param updateDTO 订单ID、目标状态等参数
     * @return 修改后的订单信息
     */
    @PostMapping("/updateStatus")
    public BaseResponse<OrderVO> updateOrderStatus(@Validated @RequestBody OrderStatusUpdateDTO updateDTO) {
        log.info("修改订单状态：{}", updateDTO);
        OrderVO orderVO = orderService.updateOrderStatus(updateDTO);
        return ResultUtils.success(orderVO);
    }

    /**
     * 用户确认订单收货
     * 更新订单确认状态为“已确认”，记录收货时间，同时将订单状态更新为“已完成”
     *
     * @param confirmReceiveDTO 确认收货参数（包含订单ID）
     * @return 更新后的订单信息
     */
    @PostMapping("/confirmReceive")
    public BaseResponse<OrderVO> confirmOrderReceive(@Validated @RequestBody OrderConfirmReceiveDTO confirmReceiveDTO) {
        log.info("用户确认订单收货：{}", confirmReceiveDTO);
        OrderVO orderVO = orderService.confirmOrderReceive(confirmReceiveDTO);
        return ResultUtils.success(orderVO);
    }

    /**
     * 用户申请订单退款
     * 验证订单状态后，更新订单为“已退款”状态，记录退款信息
     *
     * @param refundDTO 退款申请参数（订单ID、退款原因等）
     * @return 更新后的订单信息
     */
    @PostMapping("/applyRefund")
    public BaseResponse<OrderVO> applyOrderRefund(@Validated @RequestBody OrderRefundDTO refundDTO) {
        log.info("用户申请订单退款：{}", refundDTO);
        OrderVO orderVO = orderService.applyOrderRefund(refundDTO);
        return ResultUtils.success(orderVO);
    }

    /**
     * 管理员审核退款申请
     *
     * @param auditDTO 审核参数（订单ID、审核结果、备注）
     * @return 更新后的订单信息
     */
    @PostMapping("/auditRefund")
    public BaseResponse<OrderVO> auditRefund(@Validated @RequestBody OrderRefundAuditDTO auditDTO) {
        log.info("管理员审核退款申请：{}", auditDTO);
        OrderVO orderVO = orderService.auditRefund(auditDTO);
        return ResultUtils.success(orderVO);
    }

    /**
     * 商品销量Top10统计接口
     * 获取购买量前10的商品信息及销量数据
     *
     * @param timeRange 时间范围（可选，支持"30天"、"90天"、"365天"、"all"，默认all）
     * @return 销量Top10统计结果
     */
    @GetMapping("/statistics/top10Products")
    public BaseResponse<ProductSalesTop10VO> getTop10ProductSales(
            @RequestParam(required = false, defaultValue = "all") String timeRange) {
        log.info("查询商品销量Top10，时间范围：{}", timeRange);
        ProductSalesTop10VO salesTop10VO = orderService.getTop10ProductSales(timeRange);
        return ResultUtils.success(salesTop10VO);
    }

}
