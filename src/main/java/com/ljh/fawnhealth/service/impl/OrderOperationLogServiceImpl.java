package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.mapper.OrderOperationLogMapper;
import com.ljh.fawnhealth.model.entity.OrderOperationLog;
import com.ljh.fawnhealth.service.OrderOperationLogService;
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




