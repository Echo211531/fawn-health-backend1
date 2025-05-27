package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.vip.VipOrderCreateDTO;
import com.ljh.fawnhealth.model.entity.VipOrders;
import com.ljh.fawnhealth.model.vo.vip.VipOrderCreateVO;

/**
 * 会员订单服务接口
 * 提供会员订单的增删改查等业务逻辑
 */
public interface VipOrdersService extends IService<VipOrders> {

    /**
     * 创建VIP会员订单
     *
     * @param createDTO 订单创建请求参数，包含会员类型、支付方式等信息
     * @return 订单创建结果，包含订单号、金额、有效期等信息
     */
    VipOrderCreateVO createVipOrder(VipOrderCreateDTO createDTO);
}
