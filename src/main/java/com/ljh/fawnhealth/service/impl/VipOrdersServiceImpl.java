package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.VipBenefitsMapper;
import com.ljh.fawnhealth.mapper.VipOrdersMapper;
import com.ljh.fawnhealth.model.dto.vip.VipOrderCreateDTO;
import com.ljh.fawnhealth.model.entity.VipOrders;
import com.ljh.fawnhealth.model.vo.vip.VipOrderCreateVO;
import com.ljh.fawnhealth.service.VipOrdersService;
import com.ljh.fawnhealth.utils.QrCodeUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * 会员订单表服务接口
 * 提供会员订单表的增删改查等业务逻辑
 */
@Service
public class VipOrdersServiceImpl extends ServiceImpl<VipOrdersMapper, VipOrders> implements VipOrdersService {

    @Resource
    private VipBenefitsMapper vipBenefitsMapper;

    @Resource
    private VipOrdersMapper vipOrdersMapper;

    private static final String PERSONAL_PAY_URL = "https://pay.example.com";

    /**
     * 创建VIP会员订单
     *
     * @param dto 订单创建请求参数，包含会员类型、支付方式等信息
     * @return 订单创建结果，包含订单号、金额、有效期等信息
     */
    @Override
    public VipOrderCreateVO createVipOrder(VipOrderCreateDTO dto) {
        // 1. 参数校验：必填项检查
        if (dto.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (dto.getVipType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "VIP类型不能为空");
        }
        if (dto.getPaymentMethod() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "支付方式不能为空");
        }

        // 2. 查询VIP基础价格并校验
        BigDecimal basePrice = vipBenefitsMapper.selectVipPriceByType(dto.getVipType());
        if (basePrice == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "VIP类型不存在或已失效");
        }
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "VIP价格不能为负数或零");
        }

        // 3. 计算最终金额（固定无折扣）
        BigDecimal finalAmount = basePrice;

        // 4. 构建并保存订单实体
        VipOrders order = new VipOrders();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(dto.getUserId());
        order.setVipType(dto.getVipType());
        order.setAmount(basePrice);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setCouponId(null);
        order.setStatus(0);
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        order.setFinalAmount(finalAmount);

        //使用 vipOrdersMapper 插入数据
        int insertResult = vipOrdersMapper.insert(order);
        if (insertResult != 1) { // 插入成功返回1，否则失败
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据库操作失败，订单创建失败");
        }

        // 使用解析出的支付链接并拼接订单号备注
        String payUrlWithNote = buildPayUrlWithOrderNote(order.getOrderNo(), PERSONAL_PAY_URL);

        // 生成二维码（使用真实支付链接）
        String qrCodeBase64 = QrCodeUtils.generateQrCodeBase64(payUrlWithNote, 300, 300);

        // 5. 组装响应结果
        return VipOrderCreateVO.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .success(true)
                .paymentAmount(finalAmount)
                .qrCode(qrCodeBase64) // 新增二维码字段
                .discountAmount(BigDecimal.ZERO)
                .paymentUrl(payUrlWithNote)
                .build();
    }

    /**
     * 构建带订单号备注的支付链接
     * 示例：https://pay.example.com?orderNo=123&note=ORDER_123
     */
    private String buildPayUrlWithOrderNote(String orderNo, String basePayUrl) {
        if (StringUtils.isEmpty(orderNo) || StringUtils.isEmpty(basePayUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "支付链接或订单号为空");
        }

        // 根据支付平台类型拼接备注参数（微信/支付宝个人收款支持的备注参数可能不同）
        String noteParam = "&note=" + orderNo; // 示例：微信个人收款使用`note`参数，支付宝可能用`memo`
        return basePayUrl.contains("?") ? basePayUrl + noteParam : basePayUrl + "?" + noteParam;
    }
}