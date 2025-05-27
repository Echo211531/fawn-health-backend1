package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.entity.VipBenefits;
import com.ljh.fawnhealth.model.vo.vip.VipBenefitsVO;

import java.util.List;

/**
 * VIP会员权益服务接口
 * 提供会员权益的查询、管理等功能
 */
public interface VipBenefitsService extends IService<VipBenefits> {

    /**
     * 获取所有启用的会员权益列表
     *
     * @return 会员权益VO列表，包含权益名称、描述、价值等信息
     */
    List<VipBenefitsVO> getVipPrivileges();

    /**
     * 根据会员类型获取对应权益列表
     *
     * @param vipType 会员类型：1-月卡会员、2-季卡会员、3-年卡会员
     * @return 指定会员类型的权益VO列表
     */
    List<VipBenefitsVO> getVipPrivilegesByType(Integer vipType);
}