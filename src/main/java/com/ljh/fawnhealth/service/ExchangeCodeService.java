package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.model.entity.ExchangeCode;

/**
* @author 27105
* @description 针对表【exchange_code(兑换码表)】的数据库操作Service
* @createDate 2025-05-02 23:03:33
*/
public interface ExchangeCodeService extends IService<ExchangeCode> {
    void asyncGenerateCode(Coupons coupon);

    boolean updateExchangeMark(long serialNum, boolean b);
}
