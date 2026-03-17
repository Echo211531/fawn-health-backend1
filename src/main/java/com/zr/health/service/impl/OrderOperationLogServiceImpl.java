package com.zr.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.mapper.OrderOperationLogMapper;
import com.zr.health.model.entity.OrderOperationLog;
import com.zr.health.service.OrderOperationLogService;
import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【order_operation_log(订单操作日志表)】的数据库操作Service实现
* @createDate 2025-07-14 23:00:52
*/
@Service
public class OrderOperationLogServiceImpl extends ServiceImpl<OrderOperationLogMapper, OrderOperationLog>
    implements OrderOperationLogService {

}




