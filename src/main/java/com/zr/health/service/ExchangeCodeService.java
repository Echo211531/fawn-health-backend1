package com.zr.health.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.dto.coupons.ExchangeCodeQueryDTO;
import com.zr.health.model.entity.Coupons;
import com.zr.health.model.entity.ExchangeCode;
import com.zr.health.model.vo.coupons.ExchangeCodeVO;

/**
* @author 27105
* @description 针对表【exchange_code(兑换码表)】的数据库操作Service
* @createDate 2025-05-02 23:03:33
*/
public interface ExchangeCodeService extends IService<ExchangeCode> {
    void asyncGenerateCode(Coupons coupon);

    boolean updateExchangeMark(long serialNum, boolean b);

    /**
     * 分页查询兑换码
     * @param queryDTO 查询参数（含分页、状态、优惠券ID）
     * @return 分页结果
     */
    PageDTO<ExchangeCodeVO> queryExchangeCodePage(ExchangeCodeQueryDTO queryDTO);
}
