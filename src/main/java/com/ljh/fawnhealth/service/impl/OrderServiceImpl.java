package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.mapper.OrderMapper;
import com.ljh.fawnhealth.model.entity.Order;
import com.ljh.fawnhealth.service.OrderService;
import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【order(订单表)】的数据库操作Service实现
* @createDate 2025-07-14 23:00:39
*/
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
    implements OrderService {

}




