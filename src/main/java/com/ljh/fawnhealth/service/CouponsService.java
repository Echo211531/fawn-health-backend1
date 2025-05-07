package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.coupons.CouponsFormDTO;
import com.ljh.fawnhealth.model.dto.coupons.CouponsIssueFormDTO;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.model.query.coupons.CouponsQuery;
import com.ljh.fawnhealth.model.vo.coupons.CouponsDetailVO;
import com.ljh.fawnhealth.model.vo.coupons.CouponsPageVO;
import com.ljh.fawnhealth.model.vo.coupons.CouponsVO;

import java.util.List;


/**
* @author 27105
* @description 针对表【coupons(优惠券表)】的数据库操作Service
* @createDate 2025-05-02 23:02:45
*/
public interface CouponsService extends IService<Coupons> {

    /**
     * 新增优惠券
     * @param couponsFormDTO
     */
    void addCoupons(CouponsFormDTO couponsFormDTO);

    /**
     * 分页查询优惠券
     * @param query 查询条件
     * @return 分页结果
     */
    PageDTO<CouponsPageVO> queryCouponByPage(CouponsQuery query);

    /**
     * 根据ID查询优惠券
     * @param id
     * @return
     */
    CouponsDetailVO getCouponById(Long id);

    /**
     * 删除优惠券
     * @param id
     * @return
     */
    boolean deleteCoupon(Long id);

    /**
     * 修改优惠券
     * @param couponsFormDTO
     * @return
     */
    CouponsDetailVO updateCoupon(CouponsFormDTO couponsFormDTO);

    /**
     * 发放优惠券
     * @param dto
     */
    void beginIssue(CouponsIssueFormDTO dto);

    /**
     * 查询发放中的优惠券列表
     * @return
     */
    List<CouponsVO> queryIssuingCoupons(Long id);

    /**
     * 暂停发放优惠券
     * @param couponsId
     */
    void pauseIssue(Long couponsId);
}
