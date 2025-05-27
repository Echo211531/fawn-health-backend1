package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.exception.ThrowUtils;
import com.ljh.fawnhealth.mapper.VipBenefitsMapper;
import com.ljh.fawnhealth.model.entity.VipBenefits;
import com.ljh.fawnhealth.model.vo.vip.VipBenefitsVO;
import com.ljh.fawnhealth.service.VipBenefitsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * VIP会员权益服务接口实现类
 * 提供会员权益的查询、管理等功能
 */
@Service
public class VipBenefitsServiceImpl extends ServiceImpl<VipBenefitsMapper, VipBenefits>
        implements VipBenefitsService {

    @Resource
    private VipBenefitsMapper vipBenefitsMapper;

    /**
     * 获取所有启用的会员权益列表
     *
     * @return 会员权益VO列表，包含权益名称、描述、价值等信息
     */
    @Override
    public List<VipBenefitsVO> getVipPrivileges() {
        return vipBenefitsMapper.selectAllEnabledBenefits();
    }


    /**
     * 根据会员类型获取对应权益列表
     *
     * @param vipType 会员类型：1-月卡会员、2-季卡会员、3-年卡会员
     * @return 指定会员类型的权益VO列表
     */
    @Override
    public List<VipBenefitsVO> getVipPrivilegesByType(Integer vipType) {
        ThrowUtils.throwIf(vipType == null, ErrorCode.INVALID_VIP_NOT_FOUND);
        return vipBenefitsMapper.selectBenefitsByVipType(vipType);
    }
}




