package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.*;
import com.ljh.fawnhealth.model.dto.order.OrderCreateDTO;
import com.ljh.fawnhealth.model.dto.order.OrderItemDTO;
import com.ljh.fawnhealth.model.entity.*;
import com.ljh.fawnhealth.model.vo.address.AddressVO;
import com.ljh.fawnhealth.model.vo.order.OrderItemVO;
import com.ljh.fawnhealth.model.vo.order.OrderVO;
import com.ljh.fawnhealth.service.OrderService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import jakarta.annotation.Resource;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 27105
* @description 针对表【order(订单表)】的数据库操作Service实现
* @createDate 2025-07-14 23:00:39
*/
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
    implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderOperationLogMapper orderOperationLogMapper;

    @Resource
    private AddressMapper addressMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Override
    public Order getOrder(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public boolean updateOrder(Order order) {
        return orderMapper.updateById(order) > 0;
    }

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 创建成功的订单信息
     */
    @Override
    public OrderVO createOrder(OrderCreateDTO orderCreateDTO) {
        // 1. 获取用户ID
        Long userId = orderCreateDTO.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未登录，无法创建订单");
        }

        // 2. 校验收货地址是否存在且属于当前用户
        Long addressId = orderCreateDTO.getAddressId();
        Address address = addressMapper.selectById(addressId);
        if (address == null || address.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收货地址不存在或已删除");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权使用该收货地址");
        }

        // 3. 生成订单编号（规则：时间戳+随机数，确保唯一）
        String orderNo = generateOrderNo();

        // 4. 处理订单项，计算总金额并校验商品合法性
        List<OrderItemDTO> itemDTOList = orderCreateDTO.getOrderItems();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO; // 订单总金额（商品总价之和）

        for (OrderItemDTO itemDTO : itemDTOList) {
            // 4.1 校验商品是否存在（假设商品表为product）
            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null || product.getIsDelete() == 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品不存在：" + itemDTO.getProductId());
            }

            // 4.2 校验商品单价（防止前端篡改价格）
            if (itemDTO.getCurrentPrice().compareTo(product.getPrice()) != 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品价格异常：" + product.getName());
            }

            // 4.3 校验库存（假设商品表有stock字段）
            if (product.getStock() < itemDTO.getQuantity()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品库存不足：" + product.getName());
            }

            // 4.4 计算订单项总价（单价×数量）
            BigDecimal itemTotal = itemDTO.getCurrentPrice().multiply(new BigDecimal(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // 4.5 构建订单项实体
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNo(orderNo);
            orderItem.setProductId(itemDTO.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentPrice(itemDTO.getCurrentPrice());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setTotalPrice(itemTotal);
            orderItem.setSpecs(itemDTO.getSpecs());
            orderItems.add(orderItem);

            // 4.6 扣减商品库存（实际项目可能需要加锁防止超卖）
            product.setStock(product.getStock() - itemDTO.getQuantity());
            productMapper.updateById(product);
        }

        // 5. 计算订单金额（总金额+运费-优惠等）
        BigDecimal freightAmount = calculateFreight(orderItems); // 计算运费（示例：满99免运费）
        BigDecimal discountAmount = calculateDiscount(orderCreateDTO.getCouponId(), totalAmount); // 计算优惠
        BigDecimal paymentAmount = totalAmount.add(freightAmount).subtract(discountAmount);
        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            paymentAmount = BigDecimal.ZERO; // 防止金额为负数
        }

        // 6. 保存订单主表
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPaymentAmount(paymentAmount);
        order.setFreightAmount(freightAmount);
        order.setDiscountAmount(discountAmount);
        order.setPaymentType(orderCreateDTO.getPaymentType());
        order.setStatus(0); // 初始状态：待支付
        order.setNote(orderCreateDTO.getNote());
        order.setSource(orderCreateDTO.getSource());
        order.setCouponId(orderCreateDTO.getCouponId());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderMapper.insert(order);

        // 7. 保存订单项（关联订单ID）
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 8. 记录订单操作日志（创建订单）
        OrderOperationLog log = new OrderOperationLog();
        log.setOrderId(order.getId());
        log.setOrderNo(orderNo);
        log.setOperator(userId.toString()); // 操作人：当前用户ID
        log.setOperationType(1); // 1-创建订单
        log.setOperationNote("用户创建订单");
        orderOperationLogMapper.insert(log);

        // 9. 转换为VO并返回
        return convertToOrderVO(order, orderItems, address);
    }

    /**
     * 修改订单状态
     *
     * @param order
     */
    @Override
    public void updateOrderStatus(Order order) {
        // 1. 参数校验
        if (order == null || order.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }
        if (order.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单状态不能为空");
        }

        // 2. 查询原订单信息
        Order originalOrder = orderMapper.selectById(order.getId());
        if (originalOrder == null || originalOrder.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在或已删除");
        }

        // 4. 校验状态变更的合法性（防止非法状态跳转）
        validateStatusChange(originalOrder.getStatus(), order.getStatus());

        // 5. 根据目标状态更新相关字段
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(order.getStatus());
        updateOrder.setUpdateTime(new Date());

        // 针对特定状态的额外处理
        switch (order.getStatus()) {
            case 1: // 已支付待发货
                updateOrder.setPaymentTime(new Date());
                updateOrder.setPaymentType(order.getPaymentType());
                updateOrder.setPaymentSerialNumber(order.getPaymentSerialNumber());
                break;
            case 2: // 已发货
                updateOrder.setDeliveryCompany(order.getDeliveryCompany());
                updateOrder.setDeliveryNo(order.getDeliveryNo());
                updateOrder.setDeliveryTime(new Date());
                break;
            case 3: // 已完成
                updateOrder.setReceiveTime(new Date());
                updateOrder.setConfirmStatus(1); // 标记为已确认收货
                break;
            case 4: // 已取消
                // 这里可以添加恢复库存的逻辑
                restoreStock(originalOrder.getId());
                break;
            case 5: // 已退款
                updateOrder.setPaymentTime(null); // 清空支付时间
                break;
            // 其他状态可根据业务需求添加处理逻辑
        }

        // 6. 执行更新操作
        int updateRows = orderMapper.updateById(updateOrder);
        if (updateRows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单状态更新失败");
        }

        // 7. 记录订单操作日志
        OrderOperationLog log = new OrderOperationLog();
        log.setOrderId(order.getId());
        log.setOrderNo(originalOrder.getOrderNo());
        log.setOperationType(getOperationTypeByStatus(order.getStatus()));
        log.setOperationNote("订单状态从" + getStatusDesc(originalOrder.getStatus()) +
                "变更为" + getStatusDesc(order.getStatus()));
        orderOperationLogMapper.insert(log);
    }

    /**
     * 根据用户ID和订单状态查询订单列表（GET请求，无分页）
     *
     * @param userId 用户ID（必填）
     * @param status 订单状态（可选，null时查所有状态）
     * @return 订单列表
     */
    @Override
    public List<OrderVO> getOrderListByUserIdAndStatus(Long userId, Integer status) {
        // 1. 参数校验（保持不变）
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 2. 构建查询条件（保持不变）
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_delete", 0);
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("create_time");

        // 3. 查询订单列表（保持不变）
        List<Order> orderList = orderMapper.selectList(queryWrapper);

        // 4. 转换为VO列表，并填充订单项信息
        List<OrderVO> orderVOList = orderList.stream()
                .map(order -> {
                    OrderVO orderVO = new OrderVO();
                    BeanUtils.copyProperties(order, orderVO);

                    // 补充订单项信息（核心修改）
                    List<OrderItem> orderItems = orderItemMapper.selectList(
                            new QueryWrapper<OrderItem>().eq("order_id", order.getId())
                    );
                    // 转换订单项为VO
                    List<OrderItemVO> orderItemVOList = orderItems.stream()
                            .map(item -> {
                                OrderItemVO itemVO = new OrderItemVO();
                                BeanUtils.copyProperties(item, itemVO);
                                // 计算商品总价（单价×数量）
                                itemVO.setTotalPrice(item.getCurrentPrice().multiply(new BigDecimal(item.getQuantity())));
                                return itemVO;
                            })
                            .collect(Collectors.toList());

                    orderVO.setOrderItems(orderItemVOList);  // 设置订单项列表
                    orderVO.setStatusDesc(getStatusDesc(order.getStatus()));  // 状态描述
                    return orderVO;
                })
                .collect(Collectors.toList());

        return orderVOList;
    }

    /**
     * 根据订单ID查询订单详细信息
     *
     * @param orderId 订单ID（必填）
     * @return 订单详细信息（包含商品、地址等）
     */
    @Override
    public OrderVO getOrderDetailById(Long orderId) {
        // 1. 参数校验
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }

        // 2. 查询订单基本信息
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在或已删除");
        }


        // 4. 转换为VO并补充详细信息
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        // 5. 补充订单项信息（商品详情）
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", orderId)
        );
        List<OrderItemVO> orderItemVOList = orderItems.stream()
                .map(item -> {
                    OrderItemVO itemVO = new OrderItemVO();
                    BeanUtils.copyProperties(item, itemVO);
                    // 计算商品总价
                    itemVO.setTotalPrice(item.getCurrentPrice().multiply(new BigDecimal(item.getQuantity())));
                    return itemVO;
                })
                .collect(Collectors.toList());
        orderVO.setOrderItems(orderItemVOList);

        // 6. 补充收货地址信息
        Address address = addressMapper.selectById(order.getAddressId());
        if (address != null) {
            AddressVO addressVO = new AddressVO();
            BeanUtils.copyProperties(address, addressVO);
            orderVO.setAddressVO(addressVO);
        }

        // 7. 补充状态描述
        orderVO.setStatusDesc(getStatusDesc(order.getStatus()));

        return orderVO;
    }


    /**
     * 校验状态变更的合法性
     */
    private void validateStatusChange(Integer oldStatus, Integer newStatus) {
        // 状态流转规则：0→1→2→3；0→4；1→4；2→5等（根据实际业务调整）
        Set<Integer> allowStatusMap = new HashSet<>();
        switch (oldStatus) {
            case 0: // 待支付
                allowStatusMap.addAll(Arrays.asList(1, 4)); // 可变为已支付或已取消
                break;
            case 1: // 已支付待发货
                allowStatusMap.addAll(Arrays.asList(2, 4, 5)); // 可变为已发货、已取消、已退款
                break;
            case 2: // 已发货
                allowStatusMap.addAll(Arrays.asList(3, 5)); // 可变为已完成、已退款
                break;
            case 3: // 已完成
                allowStatusMap.add(5); // 可变为已退款
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前状态不允许变更");
        }
        if (!allowStatusMap.contains(newStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "不允许从" + getStatusDesc(oldStatus) + "变更为" + getStatusDesc(newStatus));
        }
    }

    /**
     * 根据状态获取操作类型（用于日志记录）
     */
    private Integer getOperationTypeByStatus(Integer status) {
        switch (status) {
            case 1: return 2; // 支付订单
            case 2: return 3; // 发货
            case 3: return 4; // 确认收货
            case 4: return 5; // 取消订单
            case 5: return 7; // 退款成功
            default: return 0;
        }
    }

    /**
     * 取消订单时恢复库存（示例）
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", orderId)
        );
        for (OrderItem item : orderItems) {
            // 这里需要根据实际商品表结构调整，增加库存
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }
    }



    /**
     * 生成订单编号（规则：时间戳+雪花ID后6位）
     */
    private String generateOrderNo() {
        return System.currentTimeMillis() + String.valueOf(IdWorker.getId()).substring(12);
    }

    /**
     * 计算运费（满1元免运费，否则10元）
     */
    private BigDecimal calculateFreight(List<OrderItem> orderItems) {
        BigDecimal total = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.compareTo(new BigDecimal("1")) >= 0 ? BigDecimal.ZERO : new BigDecimal("10");
    }

    /**
     * 计算优惠金额（示例：优惠券抵扣）
     */
    private BigDecimal calculateDiscount(Long couponId, BigDecimal totalAmount) {
        if (couponId == null) {
            return BigDecimal.ZERO;
        }
        // 实际项目中需查询优惠券表，校验有效性并计算抵扣金额
        return new BigDecimal("5"); // 假设优惠券抵扣5元
    }

    /**
     * 转换为订单VO
     */
    private OrderVO convertToOrderVO(Order order, List<OrderItem> orderItems, Address address) {
        OrderVO orderVO = BeanCopyUtils.copy(order, OrderVO.class);
        // 设置订单状态描述
        orderVO.setStatusDesc(getStatusDesc(order.getStatus()));
        // 转换收货地址VO
        AddressVO addressVO = BeanCopyUtils.copy(address, AddressVO.class);
        addressVO.setFullAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        orderVO.setAddressVO(addressVO);
        // 转换订单项VO
        List<OrderItemVO> itemVOList = BeanCopyUtils.copyList(orderItems, OrderItemVO.class);
        orderVO.setOrderItems(itemVOList);
        return orderVO;
    }

    /**
     * 获取订单状态描述
     */
    private String getStatusDesc(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付待发货";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知状态";
        }
    }
}




