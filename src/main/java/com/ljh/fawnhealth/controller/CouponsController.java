package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.model.dto.coupons.CouponsFormDTO;
import com.ljh.fawnhealth.model.dto.coupons.CouponsIssueFormDTO;
import com.ljh.fawnhealth.model.dto.user.UserLoginDTO;
import com.ljh.fawnhealth.model.query.coupons.CouponsQuery;
import com.ljh.fawnhealth.model.vo.coupons.CouponsDetailVO;
import com.ljh.fawnhealth.model.vo.coupons.CouponsPageVO;
import com.ljh.fawnhealth.model.vo.coupons.CouponsVO;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import com.ljh.fawnhealth.service.CouponsService;
import com.ljh.fawnhealth.service.EmailService;
import com.ljh.fawnhealth.service.UserCouponService;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.CharUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/coupons")
public class CouponsController {

    @Resource
    private CouponsService couponsService;

    /**
     * 新增优惠券
     * @param couponsFormDTO
     * @return
     */
    @PostMapping("/addCoupons")
    public BaseResponse<String> addCoupons(@RequestBody CouponsFormDTO couponsFormDTO){
        couponsService.addCoupons(couponsFormDTO);
        return ResultUtils.success("优惠券添加成功");
    }


//    @GetMapping("/page1")
//    public BaseResponse<Page<CouponsPageVO>> queryCouponByPage(
//            @RequestParam(defaultValue = "1") Integer pageNo,
//            @RequestParam(defaultValue = "10") Integer pageSize,
//            @RequestParam(required = false) Integer type,
//            @RequestParam(required = false) Integer status,
//            @RequestParam(required = false) String name,
//            @RequestParam(defaultValue = "false") Boolean isAsc,
//            @RequestParam(defaultValue = "createTime") String sortBy) {
//
//        CouponsQuery query = new CouponsQuery();
//        query.setPageNo(pageNo);
//        query.setPageSize(pageSize);
//        query.setType(type);
//        query.setStatus(status);
//        query.setName(name);
//        query.setIsAsc(isAsc);
//        query.setSortBy(sortBy);
//
//        return ResultUtils.success(couponsService.queryCouponByPage(query));
//    }

    /**
     * 分页查询优惠券接口
     * @param
     * @return
     */
    @PostMapping("/page")
    public BaseResponse<PageDTO<CouponsPageVO>> queryCouponByPage(@RequestBody CouponsQuery couponsQuery) {
        PageDTO<CouponsPageVO> couponsPageVOPage = couponsService.queryCouponByPage(couponsQuery);
        return ResultUtils.success(couponsPageVOPage);
    }

    /**
     * 根据ID查询优惠券
     * @param id 优惠券ID
     * @return 优惠券详情
     */
    @PostMapping("/getCouponsById")
    public BaseResponse<CouponsDetailVO> getCouponById(Long id) {
        CouponsDetailVO couponS = couponsService.getCouponById(id);
        if (couponS == null) {
            return ResultUtils.error(ErrorCode.COUPON_NOT_FOUND);
        }
        return ResultUtils.success(couponS);
    }

    /**
     * 删除优惠券
     * @param id 优惠券ID
     * @return 操作结果
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
     * 修改优惠券
     * @param couponsFormDTO 修改内容
     * @return 操作结果
     */
    @PostMapping("/updateCoupons")
    public BaseResponse<CouponsDetailVO> updateCoupon(@RequestBody CouponsFormDTO couponsFormDTO) {
        CouponsDetailVO couponsDetailVO = couponsService.updateCoupon(couponsFormDTO);
        return ResultUtils.success(couponsDetailVO);
    }

    /**
     * 发放优惠券
     * @param dto
     */
    @PostMapping("/issue")
    public BaseResponse<String> beginIssue(@RequestBody CouponsIssueFormDTO dto) {
        couponsService.beginIssue(dto);
        return ResultUtils.success("优惠券发放成功");
    }

    /**
     * 查询发放中的优惠券列表
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<CouponsVO>> queryIssuingCoupons(Long id){
        List<CouponsVO> list = couponsService.queryIssuingCoupons(id);
        return ResultUtils.success(list);
    }

    /**
     * 暂停发放优惠券
     * @param couponsId
     */
    @PostMapping("/pause")
    public BaseResponse<String> pauseIssue(Long couponsId){
        couponsService.pauseIssue(couponsId);
        return ResultUtils.success("暂停发放优惠券");
    }

}
