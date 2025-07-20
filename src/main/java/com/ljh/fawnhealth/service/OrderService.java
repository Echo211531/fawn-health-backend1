package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.order.OrderCreateDTO;
import com.ljh.fawnhealth.model.entity.Order;
import com.ljh.fawnhealth.model.vo.order.OrderVO;

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
}
