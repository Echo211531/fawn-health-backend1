package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.order.CouponDiscountDTO;
import com.ljh.fawnhealth.model.dto.order.OrderProductDTO;
import com.ljh.fawnhealth.model.vo.coupons.UserCouponsVO;
import com.ljh.fawnhealth.service.UserCouponService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户优惠券模块
 * 提供用户领取优惠券、兑换码兑换等接口
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

    /**
     * 查询用户已领取的优惠券列表
     *
     * @param userId 用户ID（必填）
     * @param status 优惠券状态（可选：0-未使用，1-已使用，2-已过期；不传表示查询全部）
     * @return 用户优惠券列表
     */
    @GetMapping("/list")
    public BaseResponse<List<UserCouponsVO>> listUserCoupons(@RequestParam("userId") Long userId,
                                                             @RequestParam(value = "status", required = false) Integer status) {
        log.info("查询用户[{}]的优惠券，状态: {}", userId, status);
        List<UserCouponsVO> couponList = userCouponService.listUserCoupons(userId, status);
        return ResultUtils.success(couponList);
    }

    /**
     * 返回可用优惠券信息
     *
     * @param orderProducts
     * @return
     */
    @PostMapping("available")
    public List<CouponDiscountDTO> findDiscountSolution(
            @RequestBody List<OrderProductDTO> orderProducts,  // 接收请求体JSON数组
            @RequestParam Long userId  // 明确从URL参数获取userId
    ) {
        return userCouponService.findDiscountSolution(orderProducts, userId);
    }
}