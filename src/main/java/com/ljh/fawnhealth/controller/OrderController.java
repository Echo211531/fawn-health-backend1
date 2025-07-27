package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.order.OrderCreateDTO;
import com.ljh.fawnhealth.model.dto.order.OrderPageQueryDTO;
import com.ljh.fawnhealth.model.dto.order.OrderStatusUpdateDTO;
import com.ljh.fawnhealth.model.vo.order.OrderAmountStatisticsVO;
import com.ljh.fawnhealth.model.vo.order.OrderChartVO;
import com.ljh.fawnhealth.model.vo.order.OrderStatisticsVO;
import com.ljh.fawnhealth.model.vo.order.OrderVO;
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
     * 订单统计接口
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
}
