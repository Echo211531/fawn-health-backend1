package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljh.fawnhealth.constant.PromotionConstants;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.model.dto.coupons.CouponsIssueFormDTO;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.entity.UserCoupon;
import com.ljh.fawnhealth.model.enums.coupons.CouponStatus;
import com.ljh.fawnhealth.model.enums.coupons.ObtainType;
import com.ljh.fawnhealth.model.enums.coupons.UserCouponStatus;
import com.ljh.fawnhealth.model.vo.coupons.CouponsDetailVO;
import com.ljh.fawnhealth.model.vo.coupons.CouponsVO;
import com.ljh.fawnhealth.service.ExchangeCodeService;
import com.ljh.fawnhealth.service.UserCouponService;
import com.ljh.fawnhealth.service.UserService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.BeanUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.exception.ThrowUtils;
import com.ljh.fawnhealth.mapper.CouponsMapper;
import com.ljh.fawnhealth.mapper.CouponsScopeMapper;
import com.ljh.fawnhealth.model.dto.coupons.CouponsFormDTO;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.model.entity.CouponsScope;
import com.ljh.fawnhealth.model.query.coupons.CouponsQuery;
import com.ljh.fawnhealth.model.vo.coupons.CouponsPageVO;
import com.ljh.fawnhealth.service.CouponsService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 27105
 * @description 针对表【coupons(优惠券表)】的数据库操作Service实现
 * @createDate 2025-05-02 23:02:45
 */
@Slf4j
@Service
public class CouponsServiceImpl extends ServiceImpl<CouponsMapper, Coupons>
        implements CouponsService {

    @Resource
    private CouponsMapper couponsMapper;

    @Resource
    private CouponsScopeMapper couponsScopeMapper;

    @Resource
    private  ExchangeCodeService codeService;

    @Resource
    private UserCouponService userCouponService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, String> redisTemplate;
    /**
     * 新增优惠券
     *
     * @param couponsFormDTO
     */
    @Override
    @Transactional
    public void addCoupons(CouponsFormDTO couponsFormDTO) {

        // 保存优惠券信息
        Coupons coupons = BeanCopyUtils.copy(couponsFormDTO, Coupons.class);
        coupons.setMaxDiscountAmount(BigDecimal.valueOf(couponsFormDTO.getMaxDiscountAmount()));
        coupons.setObtainWay(couponsFormDTO.getObtainWay().getValue());
        couponsMapper.insert(coupons);

        // 保存优惠券限定范围信息
        if (couponsFormDTO.getSpecific()) {
            Long couponsId = coupons.getId();
            List<Long> scopes = couponsFormDTO.getScopes();
            // 验证优惠券 ID 是否为空
            ThrowUtils.throwIf(scopes == null, ErrorCode.COUPON_SCOPE_NOT_FOUND);
            List<CouponsScope> couponsScopes = new ArrayList<>();
            for (Long bizId : scopes) {
                CouponsScope couponsScope = new CouponsScope();
                couponsScope.setBizId(bizId);
                couponsScope.setType(1);
                couponsScope.setCouponId(couponsId);
                couponsScopes.add(couponsScope);
            }
            int count = 0;
            for (CouponsScope couponsScope : couponsScopes) {
                count += couponsScopeMapper.insert(couponsScope);
            }
            log.info("插入数量:{}" , count);
        }
    }

    /**
     * 分页查询优惠券
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<CouponsPageVO> queryCouponByPage(CouponsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数不能为空");
        }

        // 构造分页对象
        Page<Coupons> page = new Page<>(query.getPageNo(), query.getPageSize());

        // 构造查询条件
        LambdaQueryWrapper<Coupons> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(query.getType() != null, Coupons::getDiscountType, query.getType())
                .eq(query.getStatus() != null, Coupons::getStatus, query.getStatus())
                .like(StringUtils.isNotBlank(query.getName()), Coupons::getName, query.getName())
                .orderByDesc(Coupons::getCreateTime);

        // 执行分页查询（使用 mapper）
        couponsMapper.selectPage(page, queryWrapper);

        // 转换记录为 VO
        List<CouponsPageVO> voRecords = page.getRecords().stream().map(coupons -> {
            CouponsPageVO vo = new CouponsPageVO();
            BeanUtils.copyProperties(coupons, vo);
            return vo;
        }).collect(Collectors.toList());

        // 构造 VO 分页对象
        PageDTO<CouponsPageVO> voPage = new PageDTO<>(page.getCurrent(), page.getSize(), page.getTotal());
        log.info("分页结果：总数={}, 当前页={}, 每页大小={}, 实际返回记录数={}",
                page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords().size());

        voPage.setRecords(voRecords);

        return voPage;
    }

    /**
     * 根据ID查询优惠券
     *
     * @param id
     * @return
     */
    @Override
    public CouponsDetailVO getCouponById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.COUPON_NOT_FOUND);
        Coupons coupons = couponsMapper.selectById(id);
        CouponsDetailVO vo = new CouponsDetailVO();
        BeanUtils.copyProperties(coupons, vo);
        return vo;
    }

    /**
     * 删除优惠券
     *
     * @param id
     * @return
     */
    @Override
    public boolean deleteCoupon(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.COUPON_NOT_FOUND);
        int i = couponsMapper.deleteById(id);
        if (i > 0) {
            return true;
        }
        return false;
    }


    /**
     * 修改优惠券
     *
     * @param couponsFormDTO
     * @return
     */
    @Override
    public CouponsDetailVO updateCoupon(CouponsFormDTO couponsFormDTO) {
        // 判空校验
        ThrowUtils.throwIf(couponsFormDTO == null || couponsFormDTO.getId() == null, ErrorCode.PARAMS_ERROR, "优惠券ID不能为空");

        // 查询原优惠券是否存在
        Coupons existing = couponsMapper.selectById(couponsFormDTO.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.COUPON_NOT_FOUND);

        // 拷贝并更新数据
        Coupons coupons = BeanCopyUtils.copy(couponsFormDTO, Coupons.class);
        coupons.setObtainWay(couponsFormDTO.getObtainWay().getValue());
        coupons.setDiscountType(couponsFormDTO.getDiscountType().getValue());
        int rows = couponsMapper.updateById(coupons);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "更新失败");

        // 查询修改后的优惠券并返回
        Coupons updated = couponsMapper.selectById(couponsFormDTO.getId());
        CouponsDetailVO vo = BeanCopyUtils.copy(updated, CouponsDetailVO.class);
        return vo;
    }

    /**
     * 发放优惠券
     *
     * @param dto
     */
    @Transactional
    @Override
    public void beginIssue(CouponsIssueFormDTO dto) {
        // 1.查询优惠券
        Coupons coupons = getById(dto.getId());
        ThrowUtils.throwIf(coupons == null, ErrorCode.COUPON_NOT_FOUND);

        // 2.判断优惠券状态，是否是暂停或待发放
        if (coupons.getStatus() != 1 && coupons.getStatus() != 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优惠券状态错误！");
        }

        // 3.判断是否是立刻发放
        Date issueBeginTime = dto.getIssueBeginTime();
        Date now = new Date();

        // 检查 issueBeginTime 是否为 null，或者 issueBeginTime 是否早于当前时间
        boolean isBegin = issueBeginTime == null || issueBeginTime.before(now);

        // 4.更新优惠券
        // 4.1.拷贝属性到PO
        Coupons c = BeanCopyUtils.copy(dto, Coupons.class);

        // 4.2.更新状态
        if (isBegin) {
            c.setStatus(CouponStatus.ISSUING.getValue());
            c.setIssueBeginTime(now);
        } else {
            c.setStatus(CouponStatus.UN_ISSUE.getValue());
        }

        // 4.3.写入数据库
        updateById(c);

        // 5.添加缓存，前提是立刻发放的
        if (isBegin) {
            coupons.setIssueBeginTime(c.getIssueBeginTime());
            coupons.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupons);
        }

        // 兑换码生成
        // 5.判断是否需要生成兑换码，优惠券类型必须是兑换码，优惠券状态必须是待发放
        if(coupons.getObtainWay() == 2 && coupons.getStatus() == 1){
            coupons.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupons);
        }
    }

    private void cacheCouponInfo(Coupons coupons) {
        // 1.组织数据
        Map<String, String> map = new HashMap<>(6);
        map.put("issueBeginTime", String.valueOf(coupons.getIssueBeginTime().getTime()));
        map.put("issueEndTime", String.valueOf(coupons.getIssueEndTime().getTime()));
        map.put("totalNum", String.valueOf(coupons.getTotalNum()));
        map.put("userLimit", String.valueOf(coupons.getUserLimit()));
        map.put("issueNum", String.valueOf(coupons.getIssueNum()));
        map.put("id", String.valueOf(coupons.getId()));
        map.put("termDays", coupons.getTermDays() != null ? String.valueOf(coupons.getTermDays()) : "0");
        // 打印缓存键和内容
        String cacheKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupons.getId();
        System.out.println("Cache key: " + cacheKey);
        System.out.println("Cache value: " + map);

        // 2.写缓存
        redisTemplate.opsForHash().putAll(PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupons.getId(), map);
    }


    /**
     * 查询发放中的优惠券列表
     *
     * @return
     */
    @Override
    public List<CouponsVO> queryIssuingCoupons(Long id) {
        // 1.查询发放中的优惠券列表
        List<Coupons> coupons = lambdaQuery()
                .eq(Coupons::getStatus, CouponStatus.ISSUING)
                .eq(Coupons::getObtainWay, ObtainType.PUBLIC)
                .list();

        if (coupons == null || coupons.isEmpty()) {
            return Collections.emptyList();
        }

        // 2.统计当前用户已经领取的优惠券的信息
        List<Long> couponIds = coupons.stream().map(Coupons::getId).collect(Collectors.toList());
        User user = userMapper.selectById(id);
        // 2.1.查询当前用户已经领取的优惠券的数据
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, user)
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        // 2.2.统计当前用户对优惠券的已经领取数量
        Map<Long, Long> issuedMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 2.3.统计当前用户对优惠券的已经领取并且未使用的数量
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 3.封装VO结果
        List<CouponsVO> list = new ArrayList<>(coupons.size());
        for (Coupons c : coupons) {
            // 3.1.拷贝PO属性到VO
            CouponsVO vo = BeanCopyUtils.copy(c, CouponsVO.class);
            list.add(vo);
            // 3.2.是否可以领取：已经被领取的数量 < 优惠券总数量 && 当前用户已经领取的数量 < 每人限领数量
            vo.setAvailable(
                    c.getIssueNum() < c.getTotalNum()
                            && issuedMap.getOrDefault(c.getId(), 0L) < c.getUserLimit()
            );
            // 3.3.是否可以使用：当前用户已经领取并且未使用的优惠券数量 > 0
            vo.setReceived(unusedMap.getOrDefault(c.getId(),  0L) > 0);
        }
        return list;
    }

    /**
     * 暂停发放优惠券
     * @param id
     */
    @Override
    @Transactional
    public void pauseIssue(Long id) {
        // 1.查询旧优惠券
        Coupons coupons = getById(id);
        if (coupons == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }

        // 2.当前券状态必须是未开始或进行中
        CouponStatus status = CouponStatus.of(coupons.getStatus());
        if (status != CouponStatus.UN_ISSUE && status != CouponStatus.ISSUING) {
            return;
        }

        // 3.更新状态
        boolean success = lambdaUpdate()
                .set(Coupons::getStatus, CouponStatus.PAUSE)
                .eq(Coupons::getId, id)
                .in(Coupons::getStatus, CouponStatus.UN_ISSUE, CouponStatus.ISSUING)
                .update();
        if (!success) {
            // 可能是重复更新，结束
            log.error("重复暂停优惠券");
        }

        // 4.删除缓存
        redisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id);
    }

}