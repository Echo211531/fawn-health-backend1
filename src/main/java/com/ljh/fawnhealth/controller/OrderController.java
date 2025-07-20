package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.order.OrderCreateDTO;
import com.ljh.fawnhealth.model.vo.order.OrderVO;
import com.ljh.fawnhealth.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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


}
