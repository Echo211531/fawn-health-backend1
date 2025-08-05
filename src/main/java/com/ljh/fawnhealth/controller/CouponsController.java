package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.model.dto.coupons.CouponsFormDTO;
import com.ljh.fawnhealth.model.dto.coupons.CouponsIssueFormDTO;
import com.ljh.fawnhealth.model.dto.coupons.CouponsSearchDTO;
import com.ljh.fawnhealth.model.dto.coupons.ExchangeCodeQueryDTO;
import com.ljh.fawnhealth.model.query.coupons.CouponsQuery;
import com.ljh.fawnhealth.model.vo.coupons.CouponsDetailVO;
import com.ljh.fawnhealth.model.vo.coupons.CouponsPageVO;
import com.ljh.fawnhealth.model.vo.coupons.CouponsVO;
import com.ljh.fawnhealth.model.vo.coupons.ExchangeCodeVO;
import com.ljh.fawnhealth.service.CouponsService;
import com.ljh.fawnhealth.service.ExchangeCodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 优惠券管理控制器
 * 提供优惠券的增删改查、发放管理、分页查询等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/coupons")
public class CouponsController {

    @Resource
    private CouponsService couponsService;

    @Resource
    private ExchangeCodeService exchangeCodeService;

    /**
     * 新增优惠券
     *
     * @param couponsFormDTO 优惠券表单数据（包含名称、类型、面值、有效期等信息）
     * @return 操作结果响应（成功时返回提示信息）
     */
    @PostMapping("/addCoupons")
    public BaseResponse<String> addCoupons(@RequestBody CouponsFormDTO couponsFormDTO) {
        couponsService.addCoupons(couponsFormDTO);
        return ResultUtils.success("优惠券添加成功");
    }

    /**
     * 分页查询优惠券接口
     *
     * @param couponsQuery 分页查询条件（包含页码、每页数量、类型、状态、名称、排序规则等）
     * @return 分页响应对象（包含优惠券列表及分页信息）
     */
    @PostMapping("/page")
    public BaseResponse<PageDTO<CouponsPageVO>> queryCouponByPage(@RequestBody CouponsQuery couponsQuery) {
        PageDTO<CouponsPageVO> couponsPageVOPage = couponsService.queryCouponByPage(couponsQuery);
        return ResultUtils.success(couponsPageVOPage);
    }

    /**
     * 根据ID查询优惠券详情
     *
     * @param id 优惠券ID（路径参数，不能为空）
     * @return 优惠券详情视图对象
     */
    @GetMapping("/getCouponsById")
    public BaseResponse<CouponsDetailVO> getCouponById(Long id) {
        CouponsDetailVO coupon = couponsService.getCouponById(id);
        if (coupon == null) {
            return ResultUtils.error(ErrorCode.COUPON_NOT_FOUND);
        }
        return ResultUtils.success(coupon);
    }

    /**
     * 删除优惠券（逻辑删除或物理删除，根据业务需求）
     *
     * @param id 优惠券ID（路径参数，不能为空）
     * @return 操作结果响应（成功时返回提示信息）
     */
    @PostMapping("/deleteCoupons")
    public BaseResponse<String> deleteCoupon(Long id) {
        boolean removed = couponsService.deleteCoupon(id);
        if (!removed) {
            return ResultUtils.error(ErrorCode.COUPON_NOT_FOUND);
        }
        return ResultUtils.success("删除成功");
    }

    /**
     * 修改优惠券信息
     *
     * @param couponsFormDTO 包含修改后优惠券信息的表单数据（ID必须存在）
     * @return 操作后的优惠券详情视图对象
     */
    @PostMapping("/updateCoupons")
    public BaseResponse<CouponsDetailVO> updateCoupon(@RequestBody CouponsFormDTO couponsFormDTO) {
        CouponsDetailVO updatedCoupon = couponsService.updateCoupon(couponsFormDTO);
        return ResultUtils.success(updatedCoupon);
    }

    /**
     * 发放优惠券
     *
     * @param dto 优惠券发放表单数据（包含优惠券ID、发放用户ID、发放数量等）
     * @return 操作结果响应（成功时返回提示信息）
     */
    @PostMapping("/issue")
    public BaseResponse<String> beginIssue(@RequestBody CouponsIssueFormDTO dto) {
        couponsService.beginIssue(dto);
        return ResultUtils.success("优惠券发放成功");
    }

    /**
     * 查询当前正在发放中的优惠券列表
     *
     * @param id 可选参数（如活动ID或用户ID，根据业务逻辑确定）
     * @return 正在发放的优惠券视图列表
     */
    @GetMapping("/list")
    public BaseResponse<List<CouponsVO>> queryIssuingCoupons(@RequestParam(required = false) Long id) { // 补充参数注解
        List<CouponsVO> list = couponsService.queryIssuingCoupons(id);
        return ResultUtils.success(list);
    }

    /**
     * 暂停指定优惠券的发放
     *
     * @param couponsId 待暂停发放的优惠券ID
     * @return 操作结果响应（成功时返回提示信息）
     */
    @PostMapping("/pause")
    public BaseResponse<String> pauseIssue(Long couponsId) { // 补充@RequestParam注解
        couponsService.pauseIssue(couponsId);
        return ResultUtils.success("暂停发放优惠券");
    }

    /**
     * 多条件分页查询优惠券
     * 支持按类型、状态和名称搜索
     *
     * @param queryDTO 包含查询条件和分页参数的DTO
     * @return 分页查询结果
     */
    @PostMapping("/search")
    public BaseResponse<PageDTO<CouponsPageVO>> searchCoupons(@RequestBody CouponsSearchDTO queryDTO) {
        log.info("多条件查询优惠券，参数：{}", queryDTO);

        // 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10);
        }

        PageDTO<CouponsPageVO> pageResult = couponsService.searchCoupons(queryDTO);
        return ResultUtils.success(pageResult);
    }


    /**
     * 分页查询兑换码
     *
     * @param queryDTO
     * @return
     */
    @PostMapping("/ExchangeCodepage")
    public BaseResponse<PageDTO<ExchangeCodeVO>> queryExchangeCodePage(@RequestBody ExchangeCodeQueryDTO queryDTO) {
        PageDTO<ExchangeCodeVO> page = exchangeCodeService.queryExchangeCodePage(queryDTO);
        return ResultUtils.success(page);
    }
}