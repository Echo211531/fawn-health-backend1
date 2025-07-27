package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.order.OrderCreateDTO;
import com.ljh.fawnhealth.model.dto.order.OrderPageQueryDTO;
import com.ljh.fawnhealth.model.dto.order.OrderStatusUpdateDTO;
import com.ljh.fawnhealth.model.entity.Order;
import com.ljh.fawnhealth.model.vo.order.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
* @author 27105
* @description 针对表【order(订单表)】的数据库操作Service
* @createDate 2025-07-14 23:00:39
*/
public interface OrderService extends IService<Order> {

    Order getOrder(Long orderId);
    Order getOrderByOrderNo(String orderNo);
    boolean updateOrder(Order order);

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 创建成功的订单信息
     */
    OrderVO createOrder(OrderCreateDTO orderCreateDTO);

    /**
     * 修改订单状态
     *
     * @param order
     */
    void updateOrderStatus(Order order);

    /**
     * 根据用户ID和订单状态查询订单列表（GET请求，无分页）
     *
     * @param userId 用户ID（必填）
     * @param status 订单状态（可选，null时查所有状态）
     * @return 订单列表
     */
    List<OrderVO> getOrderListByUserIdAndStatus(Long userId, Integer status);

    /**
     * 根据订单ID查询订单详细信息
     *
     * @param orderId 订单ID（必填）
     * @return 订单详细信息（包含商品、地址等）
     */
    OrderVO getOrderDetailById(Long orderId);

    /**
     * 获取订单统计数据（今日、昨日订单数及日环比）
     *
     * @return 订单统计VO
     */
    OrderStatisticsVO getOrderStatistics();

    /**
     * 统计指定时间段内的订单数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单数量
     */
    long countOrdersByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取订单金额统计数据（今日、昨日金额及日环比）
     * @return 订单金额统计VO
     */
    OrderAmountStatisticsVO getOrderAmountStatistics();

    /**
     * 统计指定时间段内的订单总金额
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单总金额
     */
    BigDecimal sumOrderAmountByTimeRange(LocalDateTime startTime, LocalDateTime endTime);


    OrderChartVO statisticOrder(String dimension);

    /**
     * 分页查询全部订单信息
     * @param queryDTO 分页查询参数
     * @return 分页订单VO列表
     */
    IPage<OrderVO> queryAllOrdersByPage(OrderPageQueryDTO queryDTO);

    /**
     * 根据订单ID查询订单项列表（包含商品信息）
     * @param orderId 订单ID
     * @return 订单项VO列表
     */
    List<OrderItemVO> getOrderItemsByOrderId(Long orderId);

    /**
     * 修改订单状态
     * @param updateDTO 包含订单ID、目标状态等信息
     * @return 修改后的订单信息
     */
    OrderVO updateOrderStatus(OrderStatusUpdateDTO updateDTO);
}
