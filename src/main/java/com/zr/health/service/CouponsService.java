package com.zr.health.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.dto.coupons.CouponsFormDTO;
import com.zr.health.model.dto.coupons.CouponsIssueFormDTO;
import com.zr.health.model.dto.coupons.CouponsSearchDTO;
import com.zr.health.model.entity.Coupons;
import com.zr.health.model.query.coupons.CouponsQuery;
import com.zr.health.model.vo.coupons.CouponsDetailVO;
import com.zr.health.model.vo.coupons.CouponsPageVO;
import com.zr.health.model.vo.coupons.CouponsVO;

import java.util.List;

/**
 * 优惠券服务接口
 * 提供优惠券的增删改查、发放管理等业务逻辑
 */
public interface CouponsService extends IService<Coupons> {

    /**
     * 创建新优惠券
     *
     * @param couponsFormDTO 优惠券表单数据传输对象，包含优惠券基本信息
     *                       如名称、类型、面值、有效期、使用规则等
     */
    void addCoupons(CouponsFormDTO couponsFormDTO);

    /**
     * 分页查询优惠券列表
     *
     * @param query 查询条件对象，包含分页参数（页码、每页数量）、筛选条件（类型、状态等）
     *              以及排序规则（如创建时间、生效时间）
     * @return 分页结果对象，包含当前页的优惠券列表及分页信息
     */
    PageDTO<CouponsPageVO> queryCouponByPage(CouponsQuery query);

    /**
     * 根据ID获取优惠券详情
     *
     * @param id 优惠券ID
     * @return 优惠券详情视图对象，包含完整的优惠券信息及关联数据
     */
    CouponsDetailVO getCouponById(Long id);

    /**
     * 删除优惠券（逻辑删除或物理删除）
     *
     * @param id 优惠券ID
     * @return 删除操作结果：true-删除成功，false-优惠券不存在或删除失败
     */
    boolean deleteCoupon(Long id);

    /**
     * 更新优惠券信息
     *
     * @param couponsFormDTO 包含更新信息的优惠券表单对象，必须包含ID
     * @return 更新后的优惠券详情视图对象
     */
    CouponsDetailVO updateCoupon(CouponsFormDTO couponsFormDTO);

    /**
     * 发放优惠券到用户账户
     *
     * @param dto 优惠券发放表单数据，包含优惠券ID、用户ID列表、发放数量等信息
     */
    void beginIssue(CouponsIssueFormDTO dto);

    /**
     * 查询正在发放中的优惠券列表
     *
     * @param id 可选参数，可能是活动ID或用户ID，根据业务场景确定筛选条件
     * @return 发放中的优惠券视图对象列表
     */
    List<CouponsVO> queryIssuingCoupons(Long id);

    /**
     * 暂停优惠券发放
     *
     * @param couponsId 待暂停发放的优惠券ID
     */
    void pauseIssue(Long couponsId);

    /**
     * 多条件分页查询优惠券
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageDTO<CouponsPageVO> searchCoupons(CouponsSearchDTO queryDTO);
}