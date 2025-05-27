package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.model.entity.UserCoupon;
import com.ljh.fawnhealth.model.vo.coupons.UserCouponsVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
* @author 27105
* @description 针对表【user_coupon(用户领取优惠券的记录，是真正使用的优惠券信息)】的数据库操作Service
* @createDate 2025-05-03 19:36:42
*/
public interface UserCouponService extends IService<UserCoupon> {

    /**
     * 领取优惠券
     * @param couponsId
     * @return
     */
    int receiveCoupon(Long couponsId,Long userId);

    @Transactional
    int checkAndCreateUserCoupon(Coupons coupons, Long userId, Integer serialNum);

    /**
     * 兑换码兑换优惠券
     * @param userId
     * @param code
     * @return
     */
    void exchangeCoupon(Long userId, String code);

    List<UserCouponsVO> listUserCoupons(Long userId, Integer status);
}
