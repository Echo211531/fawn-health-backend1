package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.*;
import com.ljh.fawnhealth.model.dto.order.OrderCreateDTO;
import com.ljh.fawnhealth.model.dto.order.OrderItemDTO;
import com.ljh.fawnhealth.model.dto.order.OrderPageQueryDTO;
import com.ljh.fawnhealth.model.dto.order.OrderStatusUpdateDTO;
import com.ljh.fawnhealth.model.entity.*;
import com.ljh.fawnhealth.model.enums.order.StatisticDimension;
import com.ljh.fawnhealth.model.vo.address.AddressVO;
import com.ljh.fawnhealth.model.vo.order.*;
import com.ljh.fawnhealth.service.AddressService;
import com.ljh.fawnhealth.service.OrderItemService;
import com.ljh.fawnhealth.service.OrderService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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
    private AddressService addressService;

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

        // 3. 生成订单编号（规则：时间戳+随机数，使用JDK自带类）
        // 生成时间戳部分（年月日时分秒，14位，如20250725201030）
        String timePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 生成随机数部分（4位，0000-9999，使用Random）
        String randomPart = String.format("%04d", new Random().nextInt(10000));

        // 拼接为订单编号（14+4=18位，如202507252010305678）
        String orderNo = timePart + randomPart;

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
        order.setAddressId(orderCreateDTO.getAddressId());
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
            // 核心：拼接省+市+区+详细地址为fullAddress
            StringBuilder fullAddress = new StringBuilder();
            // 避免空值导致的多余字符（如null拼接为"null"）
            if (StringUtils.isNotBlank(address.getProvince())) {
                fullAddress.append(address.getProvince());
            }
            if (StringUtils.isNotBlank(address.getCity())) {
                fullAddress.append(address.getCity());
            }
            if (StringUtils.isNotBlank(address.getDistrict())) {
                fullAddress.append(address.getDistrict());
            }
            if (StringUtils.isNotBlank(address.getDetailAddress())) {
                fullAddress.append(address.getDetailAddress());
            }
            addressVO.setFullAddress(fullAddress.toString());
            orderVO.setAddressVO(addressVO);
        }

        // 7. 补充状态描述
        orderVO.setStatusDesc(getStatusDesc(order.getStatus()));

        return orderVO;
    }

    /**
     * 实现订单统计逻辑
     *
     * @return
     */
    @Override
    public OrderStatisticsVO getOrderStatistics() {
        // 使用上海时区确保时间准确性
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        // 1. 今日订单：今日00:00:00至当前时间
        LocalDateTime todayStart = now.with(LocalTime.MIN);
        LocalDateTime todayEnd = now;
        long todayOrderCount = countOrdersByTimeRange(todayStart, todayEnd);

        // 2. 昨日订单：昨日00:00:00至昨日23:59:59
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = yesterdayStart.with(LocalTime.MAX);
        long yesterdayOrderCount = countOrdersByTimeRange(yesterdayStart, yesterdayEnd);

        // 3. 本月订单：本月1日00:00:00至当前时间（新增逻辑）
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime monthEnd = now;
        long monthOrderCount = countOrdersByTimeRange(monthStart, monthEnd);

        // 4. 计算日环比增长率
        BigDecimal dayOnDayRate = calculateDayOnDayRate(todayOrderCount, yesterdayOrderCount);

        // 封装结果（包含新增的本月订单数量）
        OrderStatisticsVO statisticsVO = new OrderStatisticsVO();
        statisticsVO.setTodayOrderCount(todayOrderCount);
        statisticsVO.setYesterdayOrderCount(yesterdayOrderCount);
        statisticsVO.setMonthOrderCount(monthOrderCount); // 设置新增字段
        statisticsVO.setDayOnDayRate(dayOnDayRate);
        statisticsVO.setStatisticTime(now);

        return statisticsVO;
    }


    /**
     * 订单金额统计（包含今日、昨日、本月及日环比）
     *
     * @return
     */
    @Override
    public OrderAmountStatisticsVO getOrderAmountStatistics() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        // 1. 今日订单总金额
        LocalDateTime todayStart = now.with(LocalTime.MIN);
        LocalDateTime todayEnd = now;
        BigDecimal todayAmount = sumOrderAmount(todayStart, todayEnd);

        // 2. 昨日订单总金额
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = yesterdayStart.with(LocalTime.MAX);
        BigDecimal yesterdayAmount = sumOrderAmount(yesterdayStart, yesterdayEnd);

        // 3. 本月订单总金额（新增逻辑）
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime monthEnd = now;
        BigDecimal monthAmount = sumOrderAmount(monthStart, monthEnd);

        // 4. 计算日环比增长率（今日较昨日）
        BigDecimal dayOnDayRate = calculateAmountDayOnDayRate(todayAmount, yesterdayAmount);

        // 封装结果（包含本月金额）
        OrderAmountStatisticsVO statisticsVO = new OrderAmountStatisticsVO();
        statisticsVO.setTodayOrderAmount(todayAmount);
        statisticsVO.setYesterdayOrderAmount(yesterdayAmount);
        statisticsVO.setMonthOrderAmount(monthAmount); // 设置新增字段
        statisticsVO.setDayOnDayRate(dayOnDayRate);
        statisticsVO.setStatisticTime(now);

        return statisticsVO;
    }

    /**
     * 统计指定时间段内的订单总金额（复用已有方法）
     *
     * @param startTime
     * @param endTime
     * @return
     */
    private BigDecimal sumOrderAmount(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Order::getCreateTime, startTime, endTime)
                .eq(Order::getIsDelete, 0); // 排除已删除订单

        // 调用Mapper的求和方法（或使用MyBatis-Plus的聚合查询，如前文所述）
        return orderMapper.sumTotalAmount(wrapper);
    }


    /**
     * 统计指定时间段内的订单总金额（排除已删除的订单）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return
     */
    @Override
    public BigDecimal sumOrderAmountByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(Order::getCreateTime, startTime, endTime)
                .eq(Order::getIsDelete, 0);

        // 使用mapper查询总金额，若没有数据返回BigDecimal.ZERO
        BigDecimal totalAmount = baseMapper.sumOrderAmount(queryWrapper);
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    /**
     * 计算金额日环比增长率（今日较昨日）
     *
     * @param todayAmount
     * @param yesterdayAmount
     * @return
     */
    private BigDecimal calculateAmountDayOnDayRate(BigDecimal todayAmount, BigDecimal yesterdayAmount) {
        // 处理昨日金额为0的情况
        if (yesterdayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 公式：(今日金额 - 昨日金额) / 昨日金额 * 100%，保留4位小数
        return todayAmount.subtract(yesterdayAmount)
                .divide(yesterdayAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100));
    }

    /**
     * 统计指定时间段内的订单数量（排除已删除的订单）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return
     */
    @Override
    public long countOrdersByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        // 订单创建时间在指定范围内
        queryWrapper.between(Order::getCreateTime, startTime, endTime)
                // 排除已删除的订单（根据表中is_delete字段）
                .eq(Order::getIsDelete, 0);
        return baseMapper.selectCount(queryWrapper);
    }

    /**
     * 计算日环比增长率（今日较昨日）
     *
     * @param todayCount
     * @param yesterdayCount
     * @return
     */
    private BigDecimal calculateDayOnDayRate(long todayCount, long yesterdayCount) {
        if (yesterdayCount <= 0) {
            return BigDecimal.ZERO;
        }
        // 公式：(今日数 - 昨日数) / 昨日数 * 100%，保留4位小数
        return new BigDecimal(todayCount - yesterdayCount)
                .divide(new BigDecimal(yesterdayCount), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100));
    }

    /**
     * 多维度订单统计（30天/周/月/年）
     *
     * @param dimensionDesc 前端传入的维度（如“30天”“周”）
     * @return 图表渲染数据
     */
    public OrderChartVO statisticOrder(String dimensionDesc) {
        StatisticDimension dimension = StatisticDimension.match(dimensionDesc);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        OrderChartVO result = new OrderChartVO();

        switch (dimension.getType()) {
            case "day":
                build30DayData(now, result);
                break;
            case "week":
                buildWeekData(now, result);
                break;
            case "month":
                buildMonthData(now, result);
                break;
            case "year":
                buildYearData(now, result);
                break;
        }
        return result;
    }

    // ====================== 30天统计 ======================
    private void build30DayData(LocalDateTime now, OrderChartVO result) {
        LocalDate today = now.toLocalDate();
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            dates.add(today.minusDays(29 - i)); // 生成过去30天日期
        }

        // 当前30天数据
        List<BigDecimal> currentAmounts = new ArrayList<>();
        List<Long> currentCounts = new ArrayList<>();
        // 对比周期：30天前的同期30天
        List<BigDecimal> compareAmounts = new ArrayList<>();
        List<Long> compareCounts = new ArrayList<>();
        List<String> timeLabels = new ArrayList<>();

        for (LocalDate date : dates) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            LocalDateTime compareStart = start.minusDays(30);
            LocalDateTime compareEnd = end.minusDays(30);

            timeLabels.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
            currentAmounts.add(sumAmount(start, end));
            currentCounts.add(countOrders(start, end));
            compareAmounts.add(sumAmount(compareStart, compareEnd));
            compareCounts.add(countOrders(compareStart, compareEnd));
        }

        result.setCurrentAmounts(currentAmounts);
        result.setCompareAmounts(compareAmounts);
        result.setCurrentCounts(currentCounts);
        result.setCompareCounts(compareCounts);
        result.setTimeLabels(timeLabels);
    }

    // ====================== 周统计 ======================
    private void buildWeekData(LocalDateTime now, OrderChartVO result) {
        // 本周：周一至周日
        LocalDateTime weekStart = now.with(DayOfWeek.MONDAY).with(LocalTime.MIN);
        LocalDateTime weekEnd = weekStart.plusDays(6).with(LocalTime.MAX);
        // 上周同期
        LocalDateTime lastWeekStart = weekStart.minusWeeks(1);
        LocalDateTime lastWeekEnd = weekEnd.minusWeeks(1);

        List<String> weekLabels = List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        List<BigDecimal> currentAmounts = new ArrayList<>();
        List<Long> currentCounts = new ArrayList<>();
        List<BigDecimal> compareAmounts = new ArrayList<>();
        List<Long> compareCounts = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDateTime dayStart = weekStart.plusDays(i).with(LocalTime.MIN);
            LocalDateTime dayEnd = weekStart.plusDays(i).with(LocalTime.MAX);
            LocalDateTime lastDayStart = lastWeekStart.plusDays(i).with(LocalTime.MIN);
            LocalDateTime lastDayEnd = lastWeekStart.plusDays(i).with(LocalTime.MAX);

            currentAmounts.add(sumAmount(dayStart, dayEnd));
            currentCounts.add(countOrders(dayStart, dayEnd));
            compareAmounts.add(sumAmount(lastDayStart, lastDayEnd));
            compareCounts.add(countOrders(lastDayStart, lastDayEnd));
        }

        result.setCurrentAmounts(currentAmounts);
        result.setCompareAmounts(compareAmounts);
        result.setCurrentCounts(currentCounts);
        result.setCompareCounts(compareCounts);
        result.setTimeLabels(weekLabels);
    }

    // ====================== 月统计 ======================
    private void buildMonthData(LocalDateTime now, OrderChartVO result) {
        // 本月：1号至当前
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime monthEnd = now.with(LocalTime.MAX);
        // 上月同期
        LocalDateTime lastMonthStart = monthStart.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime lastMonthEnd = monthStart.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        int monthDays = monthEnd.getDayOfMonth();
        List<String> dayLabels = new ArrayList<>();
        List<BigDecimal> currentAmounts = new ArrayList<>();
        List<Long> currentCounts = new ArrayList<>();
        List<BigDecimal> compareAmounts = new ArrayList<>();
        List<Long> compareCounts = new ArrayList<>();

        for (int i = 1; i <= monthDays; i++) {
            LocalDateTime dayStart = monthStart.plusDays(i - 1).with(LocalTime.MIN);
            LocalDateTime dayEnd = monthStart.plusDays(i - 1).with(LocalTime.MAX);
            LocalDateTime lastDayStart = lastMonthStart.plusDays(i - 1).with(LocalTime.MIN);
            LocalDateTime lastDayEnd = lastMonthStart.plusDays(i - 1).with(LocalTime.MAX);

            dayLabels.add(String.valueOf(i));
            currentAmounts.add(sumAmount(dayStart, dayEnd));
            currentCounts.add(countOrders(dayStart, dayEnd));
            compareAmounts.add(sumAmount(lastDayStart, lastDayEnd));
            compareCounts.add(countOrders(lastDayStart, lastDayEnd));
        }

        result.setCurrentAmounts(currentAmounts);
        result.setCompareAmounts(compareAmounts);
        result.setCurrentCounts(currentCounts);
        result.setCompareCounts(compareCounts);
        result.setTimeLabels(dayLabels);
    }

    // ====================== 年统计（12个月） ======================
    private void buildYearData(LocalDateTime now, OrderChartVO result) {
        // 本年：1月至当前月
        LocalDateTime yearStart = now.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
        LocalDateTime yearEnd = now.with(LocalTime.MAX);
        // 去年同期
        LocalDateTime lastYearStart = yearStart.minusYears(1).with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
        LocalDateTime lastYearEnd = yearStart.minusYears(1).with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX);

        List<String> monthLabels = List.of("一月", "二月", "三月", "四月", "五月", "六月",
                "七月", "八月", "九月", "十月", "十一月", "十二月");
        List<BigDecimal> currentAmounts = new ArrayList<>();
        List<Long> currentCounts = new ArrayList<>();
        List<BigDecimal> compareAmounts = new ArrayList<>();
        List<Long> compareCounts = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            LocalDateTime monthStart = yearStart.plusMonths(i).with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
            LocalDateTime monthEnd = yearStart.plusMonths(i).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
            LocalDateTime lastMonthStart = lastYearStart.plusMonths(i).with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
            LocalDateTime lastMonthEnd = lastYearStart.plusMonths(i).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

            currentAmounts.add(sumAmount(monthStart, monthEnd));
            currentCounts.add(countOrders(monthStart, monthEnd));
            compareAmounts.add(sumAmount(lastMonthStart, lastMonthEnd));
            compareCounts.add(countOrders(lastMonthStart, lastMonthEnd));
        }

        result.setCurrentAmounts(currentAmounts);
        result.setCompareAmounts(compareAmounts);
        result.setCurrentCounts(currentCounts);
        result.setCompareCounts(compareCounts);
        result.setTimeLabels(monthLabels);
    }

    /**
     * 统计指定时间区间的订单总金额（排除已删除）
     *
     * @param start
     * @param end
     * @return
     */
    private BigDecimal sumAmount(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Order::getCreateTime, start, end)
                .eq(Order::getIsDelete, 0); // 排除已删除订单
        // 假设 OrderMapper 有 sumTotalAmount 方法，或直接用 MyBatis-Plus 聚合查询
        return orderMapper.sumTotalAmount(wrapper);
    }


    /**
     * 统计指定时间区间的订单数量（排除已删除）
     *
     * @param start
     * @param end
     * @return
     */
    private Long countOrders(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Order::getCreateTime, start, end)
                .eq(Order::getIsDelete, 0);
        return orderMapper.selectCount(wrapper);
    }

    /**
     * 分页查询全部订单信息
     *
     * @param queryDTO 分页查询参数
     * @return
     */
    @Override
    public IPage<OrderVO> queryAllOrdersByPage(OrderPageQueryDTO queryDTO) {
        Page<Order> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getIsDelete, 0);

        // 用户ID查询条件（保持不变）
        queryWrapper.eq(queryDTO.getUserId() != null, Order::getUserId, queryDTO.getUserId());
        // 订单ID查询条件（补充：建议也加上非空判断，避免查询条件无效）
        queryWrapper.eq(queryDTO.getOrderId() != null, Order::getId, queryDTO.getOrderId());

        // 新增：支付方式查询条件（paymentType 不为空时才添加）
        queryWrapper.eq(queryDTO.getPaymentType() != null, Order::getPaymentType, queryDTO.getPaymentType());

        // 其他原有条件（保持不变）
        queryWrapper.eq(queryDTO.getStatus() != null, Order::getStatus, queryDTO.getStatus())
                .eq(queryDTO.getSource() != null, Order::getSource, queryDTO.getSource())
                .orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = baseMapper.selectPage(page, queryWrapper);
        // 转换为VO（保持原有逻辑）
        return orderPage.convert(this::convertToOrderVO);
    }


    @Override
    public List<OrderItemVO> getOrderItemsByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderItem::getOrderId, orderId) // 条件1：匹配订单ID
                .eq(OrderItem::getIsDelete, 0);      // 条件2：排除已删除的订单项

        // 查询订单项列表
        List<OrderItem> orderItems = orderItemMapper.selectList(queryWrapper);

        // 转换为VO并返回
        return orderItems.stream()
                .map(this::convertToOrderItemVO)
                .collect(Collectors.toList());
    }

    /**
     * 修改订单状态（完整实现：包含严格校验、状态流转控制、关联操作）
     *
     * @param updateDTO 包含订单ID、目标状态、操作备注等信息
     * @return 修改后的订单详情VO
     */
    @Override
    public OrderVO updateOrderStatus(OrderStatusUpdateDTO updateDTO) {
        // 1. 基础参数校验
        Long orderId = updateDTO.getOrderId();
        Integer targetStatus = updateDTO.getTargetStatus();
        if (orderId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单ID不能为空");
        }
        if (targetStatus == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标状态不能为空");
        }

        // 2. 查询原始订单信息（加锁防止并发修改，实际项目可使用selectByIdForUpdate）
        Order originalOrder = baseMapper.selectById(orderId);
        if (originalOrder == null || originalOrder.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在或已删除");
        }
        Integer currentStatus = originalOrder.getStatus();

        // 3. 校验目标状态合法性（必须是订单表定义的状态值：0-6）
        if (targetStatus < 0 || targetStatus > 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的订单状态（必须是0-6之间的值）");
        }

        // 4. 校验状态流转是否符合业务规则（核心逻辑）
        validateStatusTransition(currentStatus, targetStatus);

        // 5. 构建更新对象，仅更新必要字段（避免覆盖原有有效数据）
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(targetStatus);
        updateOrder.setUpdateTime(new Date()); // 更新时间戳

        // 6. 根据目标状态更新关联字段（传入完整DTO）
        handleStatusRelatedFields(originalOrder, updateOrder, updateDTO);

        // 7. 执行更新操作
        int updateRows = baseMapper.updateById(updateOrder);
        if (updateRows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单状态更新失败，请重试");
        }

        // 8. 记录订单操作日志（用于追踪状态变更历史）
        recordOrderOperationLog(originalOrder, targetStatus, updateDTO.getRemark());

        // 9. 返回更新后的订单详情VO
        return getOrderDetailById(orderId);
    }

    /**
     * 记录订单状态变更日志（用于审计和问题排查）
     *
     * @param order
     * @param targetStatus
     * @param remark
     */
    private void recordOrderOperationLog(Order order, Integer targetStatus, String remark) {
        OrderOperationLog operationLog = new OrderOperationLog();
        operationLog.setOrderId(order.getId());
        operationLog.setOrderNo(order.getOrderNo());
        operationLog.setOperator("system"); // 实际场景可改为操作人ID（如管理员ID）
        operationLog.setOperationType(getOperationTypeByStatus(targetStatus)); // 操作类型编码
        // 操作备注：状态变更描述 + 自定义备注
        String operationNote = String.format("订单状态从【%s】变更为【%s】",
                getStatusDesc(order.getStatus()),
                getStatusDesc(targetStatus));
        if (StringUtils.isNotBlank(remark)) {
            operationNote += "，备注：" + remark;
        }
        operationLog.setOperationNote(operationNote);
        operationLog.setCreateTime(new Date());
        orderOperationLogMapper.insert(operationLog);
    }


    /**
     * 根据目标状态更新关联字段（包含物流信息处理）
     *
     * @param originalOrder
     * @param updateOrder
     * @param updateDTO
     */
    private void handleStatusRelatedFields(Order originalOrder, Order updateOrder, OrderStatusUpdateDTO updateDTO) {
        Integer targetStatus = updateDTO.getTargetStatus();
        Date now = new Date();

        switch (targetStatus) {
            case 1: // 已支付待发货
                updateOrder.setPaymentTime(now);
                updateOrder.setPaymentSerialNumber(originalOrder.getPaymentSerialNumber());
                break;
            case 2: // 已发货（核心修改：处理物流信息）
                // 校验物流公司和单号是否为空
                if (StringUtils.isBlank(updateDTO.getDeliveryCompany())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "发货状态必须填写物流公司");
                }
                if (StringUtils.isBlank(updateDTO.getDeliveryNo())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "发货状态必须填写物流单号");
                }
                // 更新物流信息和发货时间
                updateOrder.setDeliveryCompany(updateDTO.getDeliveryCompany());
                updateOrder.setDeliveryNo(updateDTO.getDeliveryNo());
                updateOrder.setDeliveryTime(now);
                break;
            case 3: // 已完成
                updateOrder.setReceiveTime(now);
                updateOrder.setConfirmStatus(1);
                break;
            case 4: // 已取消
                restoreStock(originalOrder.getId());
                break;
            case 5: // 已退款
                updateOrder.setPaymentTime(null);
                break;
        }
    }


    /**
     * 订单项实体转VO
     *
     * @param orderItem
     * @return
     */
    private OrderItemVO convertToOrderItemVO(OrderItem orderItem) {
        OrderItemVO orderItemVO = new OrderItemVO();
        BeanUtils.copyProperties(orderItem, orderItemVO);
        return orderItemVO;
    }


    /**
     * 订单实体转VO（必须包含订单项查询）
     *
     * @param order
     * @return
     */
    private OrderVO convertToOrderVO(Order order) {
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO); // 复制基础字段

        // 关键：查询当前订单的订单项，并设置到orderItems中
        List<OrderItemVO> orderItems = getOrderItemsByOrderId(order.getId());
        orderVO.setOrderItems(orderItems); // 必须执行这一行，否则orderItems为null

        // 1. 查询收货地址（调用已有的 getAddressById 方法）
        // 注意：订单表中存储地址ID的字段是 shippingAddressId（根据前文表结构）
        AddressVO addressVO = addressService.getAddressById(order.getAddressId());
        orderVO.setAddressVO(addressVO);

        return orderVO;
    }

    /**
     * 校验状态流转是否合法（严格控制允许的状态变更路径）
     * 参考规则：
     * - 待支付（0）→ 已支付（1）、已取消（4）、已关闭（6）
     * - 已支付（1）→ 已发货（2）、已取消（4）、已退款（5）
     * - 已发货（2）→ 已完成（3）、已取消（4）
     * - 已完成（3）→ 已退款（5）
     * - 其他状态不允许直接变更
     */
    private void validateStatusTransition(Integer currentStatus, Integer targetStatus) {
        // 状态未变更，无需处理
        if (currentStatus.equals(targetStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单已是目标状态，无需重复修改");
        }

        // 定义允许的状态流转映射：key=当前状态，value=允许的目标状态列表
        Map<Integer, List<Integer>> allowedTransitions = new HashMap<>();
        allowedTransitions.put(0, Arrays.asList(1, 4, 6));    // 待支付 → 已支付、取消、关闭
        allowedTransitions.put(1, Arrays.asList(2, 4, 5));    // 已支付 → 发货、取消、退款
        allowedTransitions.put(2, Arrays.asList(3, 4));       // 已发货 → 完成、取消
        allowedTransitions.put(3, Collections.singletonList(5)); // 已完成 → 退款
        // 已取消（4）、已退款（5）、已关闭（6）不允许再变更状态
        allowedTransitions.put(4, Collections.emptyList());
        allowedTransitions.put(5, Collections.emptyList());
        allowedTransitions.put(6, Collections.emptyList());

        // 校验当前状态是否在允许的流转规则中
        List<Integer> allowedTargets = allowedTransitions.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("不允许从【%s】变更为【%s】",
                            getStatusDesc(currentStatus),
                            getStatusDesc(targetStatus)));
        }
    }


    /**
     * 校验状态变更的合法性
     *
     * @param oldStatus
     * @param newStatus
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
     *
     * @param status
     * @return
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
     *
     * @param orderId
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
     * 计算运费（满1元免运费，否则10元）
     *
     * @param orderItems
     * @return
     */
    private BigDecimal calculateFreight(List<OrderItem> orderItems) {
        BigDecimal total = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.compareTo(new BigDecimal("1")) >= 0 ? BigDecimal.ZERO : new BigDecimal("10");
    }

    /**
     * 计算优惠金额（示例：优惠券抵扣）
     *
     * @param couponId
     * @param totalAmount
     * @return
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
     *
     * @param order
     * @param orderItems
     * @param address
     * @return
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
     *
     * @param status
     * @return
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




