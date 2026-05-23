package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zr.health.constant.PromotionConstants;
import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.exception.ThrowUtils;
import com.zr.health.mapper.CouponsMapper;
import com.zr.health.mapper.CouponsScopeMapper;
import com.zr.health.mapper.UserMapper;
import com.zr.health.model.dto.coupons.CouponsFormDTO;
import com.zr.health.model.dto.coupons.CouponsIssueFormDTO;
import com.zr.health.model.dto.coupons.CouponsSearchDTO;
import com.zr.health.model.entity.Coupons;
import com.zr.health.model.entity.CouponsScope;
import com.zr.health.model.entity.UserCoupon;
import com.zr.health.model.enums.coupons.CouponStatus;
import com.zr.health.model.enums.coupons.ObtainType;
import com.zr.health.model.query.coupons.CouponsQuery;
import com.zr.health.model.vo.coupons.CouponsDetailVO;
import com.zr.health.model.vo.coupons.CouponsPageVO;
import com.zr.health.model.vo.coupons.CouponsVO;
import com.zr.health.service.CouponsService;
import com.zr.health.service.ExchangeCodeService;
import com.zr.health.service.UserCouponService;
import com.zr.health.utils.BeanCopyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 优惠券服务接口实现类
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
    private ExchangeCodeService codeService;

    @Resource
    private UserCouponService userCouponService;

    @Resource
    private UserMapper userMapper;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public void addCoupons(CouponsFormDTO couponsFormDTO) {
        Coupons coupons = BeanCopyUtils.copy(couponsFormDTO, Coupons.class);
        coupons.setDiscountType(couponsFormDTO.getDiscountType());
        Integer maxDiscountAmount = couponsFormDTO.getMaxDiscountAmount();
        coupons.setMaxDiscountAmount(maxDiscountAmount == null ? BigDecimal.ZERO : BigDecimal.valueOf(maxDiscountAmount.longValue()));
        log.info("添加优惠券最大的优惠金额是：{}", coupons.getMaxDiscountAmount());
        coupons.setObtainWay(couponsFormDTO.getObtainWay());
        if (coupons.getCode() == null || coupons.getCode().trim().isEmpty()) {
            coupons.setCode(UUID.randomUUID().toString().replace("-", ""));
        }
        couponsMapper.insert(coupons);

        if (Boolean.TRUE.equals(couponsFormDTO.getSpecific())) {
            Long couponsId = coupons.getId();
            List<Long> scopes = couponsFormDTO.getScopes();
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
            log.info("插入优惠券范围记录数量:{}", count);
        }
    }

    @Override
    public PageDTO<CouponsPageVO> queryCouponByPage(CouponsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数不能为空");
        }

        Page<Coupons> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<Coupons> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Coupons::getIsDelete, 0)
                .eq(query.getType() != null, Coupons::getDiscountType, query.getType())
                .eq(query.getStatus() != null, Coupons::getStatus, query.getStatus())
                .like(StringUtils.isNotBlank(query.getName()), Coupons::getName, query.getName())
                .orderByDesc(Coupons::getCreateTime);

        couponsMapper.selectPage(page, queryWrapper);

        List<CouponsPageVO> voRecords = page.getRecords().stream().map(coupons -> {
            CouponsPageVO vo = new CouponsPageVO();
            BeanUtils.copyProperties(coupons, vo);
            return vo;
        }).collect(Collectors.toList());

        PageDTO<CouponsPageVO> voPage = new PageDTO<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

    @Override
    public CouponsDetailVO getCouponById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.COUPON_NOT_FOUND);
        Coupons coupons = couponsMapper.selectById(id);
        CouponsDetailVO vo = new CouponsDetailVO();
        BeanCopyUtils.copy(coupons, vo);
        return vo;
    }

    @Override
    public boolean deleteCoupon(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.COUPON_NOT_FOUND);
        int i = couponsMapper.logicDeleteById(id);
        return i > 0;
    }

    @Override
    public CouponsDetailVO updateCoupon(CouponsFormDTO couponsFormDTO) {
        ThrowUtils.throwIf(couponsFormDTO == null || couponsFormDTO.getId() == null, ErrorCode.PARAMS_ERROR, "优惠券ID不能为空");
        Coupons existing = couponsMapper.selectById(couponsFormDTO.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.COUPON_NOT_FOUND);

        Coupons coupons = BeanCopyUtils.copy(couponsFormDTO, Coupons.class);
        coupons.setObtainWay(couponsFormDTO.getObtainWay());
        coupons.setDiscountType(couponsFormDTO.getDiscountType());
        int rows = couponsMapper.updateById(coupons);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "更新失败");

        Coupons updated = couponsMapper.selectById(couponsFormDTO.getId());
        CouponsDetailVO vo = BeanCopyUtils.copy(updated, CouponsDetailVO.class);
        return vo;
    }

    @Override
    @Transactional
    public void beginIssue(CouponsIssueFormDTO dto) {
        Coupons coupons = getById(dto.getId());
        ThrowUtils.throwIf(coupons == null, ErrorCode.COUPON_NOT_FOUND);

        if (coupons.getStatus() != 1 && coupons.getStatus() != 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优惠券状态错误！");
        }

        Date issueBeginTime = dto.getIssueBeginTime();
        Date now = new Date();
        boolean isBegin = issueBeginTime == null;

        Coupons c = new Coupons();
        c.setId(dto.getId());
        c.setIssueEndTime(dto.getIssueEndTime());
        c.setTermDays(dto.getTermDays());
        c.setTermBeginTime(dto.getTermBeginTime());
        c.setTermEndTime(dto.getTermEndTime());

        if (isBegin) {
            c.setStatus(3);
            c.setIssueBeginTime(now);
        } else {
            c.setStatus(2);
            c.setIssueBeginTime(dto.getIssueBeginTime());
        }

        updateById(c);

        if (isBegin) {
            coupons.setIssueBeginTime(c.getIssueBeginTime());
            coupons.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupons);
        }

        if (coupons.getObtainWay() == 2 && coupons.getStatus() == 1) {
            coupons.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupons);
        }
    }

    @Override
    public void pauseIssue(Long couponsId) {
        ThrowUtils.throwIf(couponsId == null, ErrorCode.COUPON_NOT_FOUND);
        Coupons coupons = getById(couponsId);
        ThrowUtils.throwIf(coupons == null, ErrorCode.COUPON_NOT_FOUND);

        Coupons update = new Coupons();
        update.setId(couponsId);
        update.setStatus(5);
        updateById(update);
    }

    private void cacheCouponInfo(Coupons coupons) {
        Map<String, String> map = new HashMap<>(8);
        map.put("issueBeginTime", String.valueOf(coupons.getIssueBeginTime().getTime()));
        map.put("issueEndTime", String.valueOf(coupons.getIssueEndTime().getTime()));
        map.put("totalNum", String.valueOf(coupons.getTotalNum()));
        map.put("userLimit", String.valueOf(coupons.getUserLimit()));
        map.put("issueNum", String.valueOf(coupons.getIssueNum()));
        map.put("id", String.valueOf(coupons.getId()));
        map.put("termDays", coupons.getTermDays() != null ? String.valueOf(coupons.getTermDays()) : "0");

        String cacheKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupons.getId();
        String stockKey = cacheKey + ":stock";
        int remainingStock = Math.max(coupons.getTotalNum() - coupons.getIssueNum(), 0);

        log.info("Cache coupon key: {}", cacheKey);
        log.info("Cache stock key: {}, stock: {}", stockKey, remainingStock);

        stringRedisTemplate.opsForHash().putAll(cacheKey, map);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(remainingStock));
    }

    @Override
    public List<CouponsVO> queryIssuingCoupons(Long id) {
        List<Coupons> coupons = lambdaQuery()
                .eq(Coupons::getIsDelete, 0)
                .eq(Coupons::getStatus, CouponStatus.ISSUING)
                .eq(Coupons::getObtainWay, ObtainType.PUBLIC)
                .list();

        if (coupons == null || coupons.isEmpty()) {
            return Collections.emptyList();
        }

        List<CouponsVO> vos = coupons.stream()
                .map(item -> BeanCopyUtils.copy(item, CouponsVO.class))
                .collect(Collectors.toList());

        if (id != null) {
            List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                    .eq(UserCoupon::getUserId, id)
                    .in(UserCoupon::getCouponId, coupons.stream().map(Coupons::getId).collect(Collectors.toList()))
                    .list();
            Map<Long, Long> countMap = userCoupons.stream()
                    .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
            for (CouponsVO vo : vos) {
                vo.setReceived(countMap.getOrDefault(vo.getId(), 0L) > 0);
            }
        }

        return vos;
    }

    @Override
    public PageDTO<CouponsPageVO> searchCoupons(CouponsSearchDTO queryDTO) {
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询参数不能为空");

        Page<Coupons> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Coupons> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupons::getIsDelete, 0)
                .eq(queryDTO.getDiscountType() != null, Coupons::getDiscountType, queryDTO.getDiscountType())
                .eq(queryDTO.getStatus() != null, Coupons::getStatus, queryDTO.getStatus())
                .like(StringUtils.isNotBlank(queryDTO.getName()), Coupons::getName, queryDTO.getName())
                .orderByDesc(Coupons::getCreateTime);

        couponsMapper.selectPage(page, wrapper);

        List<CouponsPageVO> records = page.getRecords().stream().map(coupon -> {
            CouponsPageVO vo = new CouponsPageVO();
            BeanUtils.copyProperties(coupon, vo);
            return vo;
        }).collect(Collectors.toList());

        PageDTO<CouponsPageVO> result = new PageDTO<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void updateCouponStatus() {
        Date now = new Date();
        lambdaUpdate()
                .eq(Coupons::getStatus, CouponStatus.UN_ISSUE)
                .le(Coupons::getIssueBeginTime, now)
                .set(Coupons::getStatus, CouponStatus.ISSUING)
                .update();
    }
}
