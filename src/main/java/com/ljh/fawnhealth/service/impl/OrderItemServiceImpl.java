package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.mapper.OrderItemMapper;
import com.ljh.fawnhealth.model.entity.OrderItem;
import com.ljh.fawnhealth.service.OrderItemService;
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




