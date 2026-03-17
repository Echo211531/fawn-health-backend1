package com.zr.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.mapper.OrderItemMapper;
import com.zr.health.model.entity.OrderItem;
import com.zr.health.service.OrderItemService;
import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【order_item(订单商品明细表)】的数据库操作Service实现
* @createDate 2025-07-14 23:00:48
*/
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem>
    implements OrderItemService {

}




