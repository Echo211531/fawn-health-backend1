package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.vip.VipOrderCreateDTO;
import com.ljh.fawnhealth.model.vo.vip.VipBenefitsVO;
import com.ljh.fawnhealth.model.vo.vip.VipOrderCreateVO;
import com.ljh.fawnhealth.service.VipBenefitsService;
import com.ljh.fawnhealth.service.VipOrdersService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * VIP会员模块
 * 提供VIP会员的下单等接口
 */
@Slf4j
@RestController
@RequestMapping("/vip")
public class VipController {

    @Resource
    private VipOrdersService vipOrderService;

    @Resource
    private VipBenefitsService vipBenefitsService;

    /**
     * 创建VIP会员订单
     *
     * @param createDTO 订单创建请求参数，包含会员类型、支付方式等信息
     * @return 订单创建结果，包含订单号、金额、有效期等信息
     */
    @PostMapping("/createOrder")
    public BaseResponse<VipOrderCreateVO> createVipOrder(@RequestBody VipOrderCreateDTO createDTO) {
        log.info("创建VIP订单: {}", createDTO);
        // 调用服务层创建订单
        VipOrderCreateVO response = vipOrderService.createVipOrder(createDTO);
        // 封装通用返回结果
        return ResultUtils.success(response);
    }

    /**
     * 获取会员权益列表
     *
     * @return 会员权益列表，包含所有可用的会员特权信息
     */
    @GetMapping("/privileges")
    public BaseResponse<List<VipBenefitsVO>> getVipPrivileges() {
        log.info("获取会员权益列表");
        // 调用服务层获取权益列表
        List<VipBenefitsVO> privileges = vipBenefitsService.getVipPrivileges();
        // 封装通用返回结果
        return ResultUtils.success(privileges);
    }

    /**
     * 根据会员类型获取对应权益列表
     *
     * @param vipType 会员类型：1-月卡，2-季卡，3-年卡
     * @return 指定会员类型的权益列表
     */
    @GetMapping("/privileges/{vipType}")
    public BaseResponse<List<VipBenefitsVO>> getVipPrivilegesByType(@PathVariable Integer vipType) {
        log.info("获取会员类型为{}的权益列表", vipType);
        // 调用服务层获取指定类型的权益列表
        List<VipBenefitsVO> privileges = vipBenefitsService.getVipPrivilegesByType(vipType);
        // 封装通用返回结果
        return ResultUtils.success(privileges);
    }
}