package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.service.UserCouponService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user-coupon")
public class UserCouponController {

    @Resource
    private UserCouponService userCouponService;

    /**
     * 领取优惠券
     * @param couponsId
     */
    @PostMapping("/receive")
    public BaseResponse<String> receiveCoupon(Long couponsId,Long userId){
        int i =  userCouponService.receiveCoupon(couponsId,userId);
        if (i > 0){
            return ResultUtils.success("优惠券领取成功");
        }
        return ResultUtils.success("优惠券领取失败");
    }

    /**
     * 兑换码兑换优惠券
     * @param code
     */
    @PostMapping("/exchange")
    public BaseResponse<String> exchangeCoupon(Long userId, String code){
        userCouponService.exchangeCoupon(userId, code);
        return ResultUtils.success("优惠券兑换成功");
    }
}
