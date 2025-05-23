package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.service.UserCouponService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户优惠券操作控制器
 * 处理用户领取优惠券、兑换码兑换等业务逻辑
 */
@Slf4j
@RestController
@RequestMapping("/userCoupon")
public class UserCouponController {

    @Resource
    private UserCouponService userCouponService;

    /**
     * 领取公开优惠券接口
     *
     * @param couponsId 优惠券ID（必填，需存在且处于可领取状态）
     * @param userId    用户ID（必填，需为有效用户）
     * @return 操作结果：成功返回提示信息，失败返回错误状态
     */
    @PostMapping("/receive")
    public BaseResponse<String> receiveCoupon(@RequestParam("couponsId") Long couponsId,
                                              @RequestParam("userId") Long userId) {
        log.info("用户[{}]尝试领取优惠券[{}]", userId, couponsId);

        int result = userCouponService.receiveCoupon(couponsId, userId);

        if (result > 0) {
            log.info("优惠券领取成功：用户[{}]，优惠券[{}]", userId, couponsId);
            return ResultUtils.success("优惠券领取成功");
        } else {
            log.warn("优惠券领取失败：用户[{}]，优惠券[{}]", userId, couponsId);
            return ResultUtils.success("优惠券领取失败");
        }
    }

    /**
     * 兑换码兑换优惠券接口
     *
     * @param userId 用户ID（必填，需为有效用户）
     * @param code   兑换码（必填，需为有效且未使用的兑换码）
     * @return 操作结果：成功返回提示信息，失败抛出对应异常
     */
    @PostMapping("/exchange")
    public BaseResponse<String> exchangeCoupon(@RequestParam("userId") Long userId,
                                               @RequestParam("code") String code) {
        log.info("用户[{}]尝试使用兑换码[{}]兑换优惠券", userId, code);

        userCouponService.exchangeCoupon(userId, code);

        log.info("兑换码兑换成功：用户[{}]，兑换码[{}]", userId, code);
        return ResultUtils.success("优惠券兑换成功");
    }
}