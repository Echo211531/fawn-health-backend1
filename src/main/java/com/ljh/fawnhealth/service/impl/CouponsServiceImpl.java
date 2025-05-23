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
 * 优惠券服务接口实现类
 * 提供优惠券的增删改查、发放管理等业务逻辑
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
     * @param couponsFormDTO 优惠券表单数据，包含基础信息及适用范围
     */
    @Override
    @Transactional
    public void addCoupons(CouponsFormDTO couponsFormDTO) {

        // 保存优惠券信息
        Coupons coupons = BeanCopyUtils.copy(couponsFormDTO, Coupons.class);
        coupons.setMaxDiscountAmount(BigDecimal.valueOf(couponsFormDTO.getMaxDiscountAmount()));
        coupons.setObtainWay(couponsFormDTO.getObtainWay().getValue());
        couponsMapper.insert(coupons);

        // 保存优惠券限定范围信息（若为特定范围优惠券）
        if (couponsFormDTO.getSpecific()) {
            Long couponsId = coupons.getId();
            List<Long> scopes = couponsFormDTO.getScopes();
            // 验证适用范围是否为空
            ThrowUtils.throwIf(scopes == null, ErrorCode.COUPON_SCOPE_NOT_FOUND);
            List<CouponsScope> couponsScopes = new ArrayList<>();
            for (Long bizId : scopes) {
                CouponsScope couponsScope = new CouponsScope();
                couponsScope.setBizId(bizId);
                couponsScope.setType(1); // type=1表示业务ID范围
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

    /**
     * 分页查询优惠券
     *
     * @param query 查询条件对象，包含分页参数、类型、状态、名称等筛选条件
     * @return 分页结果对象，包含优惠券视图列表及分页信息
     */
    @Override
    public PageDTO<CouponsPageVO> queryCouponByPage(CouponsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数不能为空");
        }

        // 构造分页对象
        Page<Coupons> page = new Page<>(query.getPageNo(), query.getPageSize());

        // 构造查询条件（类型、状态、名称模糊查询、按创建时间倒序）
        LambdaQueryWrapper<Coupons> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(query.getType() != null, Coupons::getDiscountType, query.getType())
                .eq(query.getStatus() != null, Coupons::getStatus, query.getStatus())
                .like(StringUtils.isNotBlank(query.getName()), Coupons::getName, query.getName())
                .orderByDesc(Coupons::getCreateTime);

        // 执行分页查询
        couponsMapper.selectPage(page, queryWrapper);

        // 将实体对象转换为视图对象
        List<CouponsPageVO> voRecords = page.getRecords().stream().map(coupons -> {
            CouponsPageVO vo = new CouponsPageVO();
            BeanUtils.copyProperties(coupons, vo);
            return vo;
        }).collect(Collectors.toList());

        // 构造分页结果对象
        PageDTO<CouponsPageVO> voPage = new PageDTO<>(page.getCurrent(), page.getSize(), page.getTotal());
        log.info("分页结果：总数={}, 当前页={}, 每页大小={}, 实际返回记录数={}",
                page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords().size());

        voPage.setRecords(voRecords);

        return voPage;
    }

    /**
     * 根据ID查询优惠券详情
     *
     * @param id 优惠券ID
     * @return 优惠券详情视图对象
     */
    @Override
    public CouponsDetailVO getCouponById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.COUPON_NOT_FOUND); // 校验ID非空
        Coupons coupons = couponsMapper.selectById(id);
        CouponsDetailVO vo = new CouponsDetailVO();
        BeanUtils.copyProperties(coupons, vo);
        return vo;
    }

    /**
     * 删除优惠券（物理删除）
     *
     * @param id 优惠券ID
     * @return 删除成功与否
     */
    @Override
    public boolean deleteCoupon(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.COUPON_NOT_FOUND); // 校验ID非空
        int i = couponsMapper.deleteById(id);
        return i > 0;
    }

    /**
     * 修改优惠券信息
     *
     * @param couponsFormDTO 包含修改后信息的表单对象（必须包含ID）
     * @return 修改后的优惠券详情视图对象
     */
    @Override
    public CouponsDetailVO updateCoupon(CouponsFormDTO couponsFormDTO) {
        // 校验参数非空及ID存在
        ThrowUtils.throwIf(couponsFormDTO == null || couponsFormDTO.getId() == null, ErrorCode.PARAMS_ERROR, "优惠券ID不能为空");

        // 查询原优惠券是否存在
        Coupons existing = couponsMapper.selectById(couponsFormDTO.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.COUPON_NOT_FOUND);

        // 拷贝更新数据并转换枚举值
        Coupons coupons = BeanCopyUtils.copy(couponsFormDTO, Coupons.class);
        coupons.setObtainWay(couponsFormDTO.getObtainWay().getValue());
        coupons.setDiscountType(couponsFormDTO.getDiscountType().getValue());
        int rows = couponsMapper.updateById(coupons);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "更新失败");

        // 返回最新数据
        Coupons updated = couponsMapper.selectById(couponsFormDTO.getId());
        CouponsDetailVO vo = BeanCopyUtils.copy(updated, CouponsDetailVO.class);
        return vo;
    }

    /**
     * 发放优惠券（核心业务逻辑）
     *
     * @param dto 发放参数，包含优惠券ID、发放时间、用户限制等
     */
    @Transactional
    @Override
    public void beginIssue(CouponsIssueFormDTO dto) {
        // 1. 查询优惠券信息
        Coupons coupons = getById(dto.getId());
        ThrowUtils.throwIf(coupons == null, ErrorCode.COUPON_NOT_FOUND);

        // 2. 校验优惠券状态（仅允许待发放或暂停状态）
        if (coupons.getStatus() != 1 && coupons.getStatus() != 5) { // 1=待发放，5=暂停（对应枚举值）
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优惠券状态错误！");
        }

        // 3. 判断是否立即发放（根据发放开始时间）
        Date issueBeginTime = dto.getIssueBeginTime();
        Date now = new Date();
        boolean isBegin = issueBeginTime == null || issueBeginTime.before(now);

        // 4. 更新优惠券状态及时间
        Coupons c = BeanCopyUtils.copy(dto, Coupons.class);
        if (isBegin) {
            c.setStatus(CouponStatus.ISSUING.getValue()); // 发放中状态
            c.setIssueBeginTime(now);
        } else {
            c.setStatus(CouponStatus.UN_ISSUE.getValue()); // 待发放状态
        }
        updateById(c);

        // 5. 立即发放时缓存优惠券信息
        if (isBegin) {
            coupons.setIssueBeginTime(c.getIssueBeginTime());
            coupons.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupons);
        }

        // 6. 生成兑换码（若为兑换码类型且状态为待发放）
        if (coupons.getObtainWay() == 2 && coupons.getStatus() == 1) { // 2=兑换码类型，1=待发放状态
            coupons.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupons);
        }
    }

    /**
     * 缓存优惠券发放信息到Redis
     *
     * @param coupons 待缓存的优惠券对象
     */
    private void cacheCouponInfo(Coupons coupons) {
        // 1. 组织缓存数据（包含发放时间、数量限制等关键信息）
        Map<String, String> map = new HashMap<>(6);
        map.put("issueBeginTime", String.valueOf(coupons.getIssueBeginTime().getTime()));
        map.put("issueEndTime", String.valueOf(coupons.getIssueEndTime().getTime()));
        map.put("totalNum", String.valueOf(coupons.getTotalNum()));
        map.put("userLimit", String.valueOf(coupons.getUserLimit()));
        map.put("issueNum", String.valueOf(coupons.getIssueNum()));
        map.put("id", String.valueOf(coupons.getId()));
        map.put("termDays", coupons.getTermDays() != null ? String.valueOf(coupons.getTermDays()) : "0");
        // 打印缓存键值对（调试用）
        String cacheKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupons.getId();
        System.out.println("Cache key: " + cacheKey);
        System.out.println("Cache value: " + map);

        // 2. 写入Redis缓存（使用Hash结构）
        redisTemplate.opsForHash().putAll(PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupons.getId(), map);
    }

    /**
     * 查询发放中的优惠券列表（含用户领取状态）
     *
     * @param id 用户ID（可选，用于查询用户领取情况）
     * @return 发放中的优惠券视图列表
     */
    @Override
    public List<CouponsVO> queryIssuingCoupons(Long id) {
        // 1. 查询所有状态为“发放中”且领取方式为“公开领取”的优惠券
        List<Coupons> coupons = lambdaQuery()
                .eq(Coupons::getStatus, CouponStatus.ISSUING) // 发放中状态
                .eq(Coupons::getObtainWay, ObtainType.PUBLIC) // 公开领取类型
                .list();

        if (coupons == null || coupons.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 统计用户领取记录（若传入用户ID）
        List<Long> couponIds = coupons.stream().map(Coupons::getId).collect(Collectors.toList());
        User user = id != null ? userMapper.selectById(id) : null;
        List<UserCoupon> userCoupons = Collections.emptyList();
        if (user != null) {
            // 查询用户已领取的优惠券
            userCoupons = userCouponService.lambdaQuery()
                    .eq(UserCoupon::getUserId, user.getId()) // 关联用户ID
                    .in(UserCoupon::getCouponId, couponIds) // 关联优惠券ID列表
                    .list();
        }

        // 2.1 统计已领取数量和未使用数量
        Map<Long, Long> issuedMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED) // 未使用状态
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        // 3. 封装视图对象并计算领取状态
        List<CouponsVO> list = new ArrayList<>(coupons.size());
        for (Coupons c : coupons) {
            CouponsVO vo = BeanCopyUtils.copy(c, CouponsVO.class);
            // 是否可领取：剩余库存 > 0 且 用户未超过领取限制
            vo.setAvailable(
                    c.getIssueNum() < c.getTotalNum()
                            && issuedMap.getOrDefault(c.getId(), 0L) < c.getUserLimit()
            );
            // 是否已领取未使用：存在未使用记录
            vo.setReceived(unusedMap.getOrDefault(c.getId(), 0L) > 0);
            list.add(vo);
        }
        return list;
    }

    /**
     * 暂停发放优惠券
     *
     * @param id 优惠券ID
     */
    @Override
    @Transactional
    public void pauseIssue(Long id) {
        // 1. 查询优惠券信息
        Coupons coupons = getById(id);
        if (coupons == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }

        // 2. 校验状态（仅允许未开始或进行中状态暂停）
        CouponStatus status = CouponStatus.of(coupons.getStatus());
        if (status != CouponStatus.UN_ISSUE && status != CouponStatus.ISSUING) { // UN_ISSUE=待发放，ISSUING=发放中
            return;
        }

        // 3. 更新状态为“暂停”
        boolean success = lambdaUpdate()
                .set(Coupons::getStatus, CouponStatus.PAUSE) // 暂停状态
                .eq(Coupons::getId, id)
                .in(Coupons::getStatus, CouponStatus.UN_ISSUE, CouponStatus.ISSUING) // 仅允许从待发放或发放中状态暂停
                .update();
        if (!success) {
            log.error("重复暂停优惠券，ID:{}", id); // 记录重复操作日志
        }

        // 4. 清理缓存
        redisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id);
    }

}