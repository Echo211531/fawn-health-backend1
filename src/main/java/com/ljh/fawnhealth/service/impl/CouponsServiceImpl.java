package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljh.fawnhealth.constant.PromotionConstants;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.model.dto.coupons.CouponsIssueFormDTO;
import com.ljh.fawnhealth.model.dto.coupons.CouponsSearchDTO;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.entity.UserCoupon;
import com.ljh.fawnhealth.model.enums.coupons.CouponStatus;
import com.ljh.fawnhealth.model.enums.coupons.DiscountType;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        coupons.setDiscountType(couponsFormDTO.getDiscountType());
        coupons.setMaxDiscountAmount(BigDecimal.valueOf(couponsFormDTO.getMaxDiscountAmount()));
        log.info("添加优惠券最大的优惠金额是：{}" ,BigDecimal.valueOf(couponsFormDTO.getMaxDiscountAmount()));
        coupons.setObtainWay(couponsFormDTO.getObtainWay());
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
        BeanCopyUtils.copy(coupons, vo);
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
        // 执行逻辑删除，更新 is_delete 字段为 1
        int i = couponsMapper.logicDeleteById(id);
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
        coupons.setObtainWay(couponsFormDTO.getObtainWay());
        coupons.setDiscountType(couponsFormDTO.getDiscountType());
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
        if (coupons.getStatus() != 1 && coupons.getStatus() != 5) { // 1=待发放，5=暂停
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优惠券状态错误！");
        }

        // 3. 判断是否立即发放（根据发放开始时间）
        Date issueBeginTime = dto.getIssueBeginTime();
        Date now = new Date();

        // 修改判断逻辑：立即发放 = 开始时间为null 或者 开始时间早于等于当前时间
        boolean isBegin = issueBeginTime == null;

        log.info("=== 发放设置调试信息 ===");
        log.info("接收到的开始时间: {}", issueBeginTime);
        log.info("当前时间: {}", now);
        log.info("开始时间是否为null: {}", issueBeginTime == null);
        if (issueBeginTime != null) {
            log.info("开始时间毫秒: {}", issueBeginTime.getTime());
            log.info("当前时间毫秒: {}", now.getTime());
            log.info("时间差(毫秒): {}", now.getTime() - issueBeginTime.getTime());
            log.info("开始时间是否早于等于当前时间: {}", issueBeginTime.getTime() <= now.getTime());
        }
        log.info("是否立即发放: {}", isBegin);
        log.info("========================");

        // 4. 更新优惠券状态及时间
        Coupons c = new Coupons();
        c.setId(dto.getId());
        c.setIssueEndTime(dto.getIssueEndTime());
        c.setTermDays(dto.getTermDays());
        c.setTermBeginTime(dto.getTermBeginTime());
        c.setTermEndTime(dto.getTermEndTime());

        if (isBegin) {
            c.setStatus(3); // 3=进行中（立即发放）
            c.setIssueBeginTime(now);
            log.info("设置为立即发放，状态: 3(进行中), 开始时间: {}", now);
        } else {
            c.setStatus(2); // 2=未开始（定时发放）
            c.setIssueBeginTime(dto.getIssueBeginTime());
            log.info("设置为定时发放，状态: 2(未开始), 开始时间: {}", dto.getIssueBeginTime());
        }

        updateById(c);

        // 5. 立即发放时缓存优惠券信息
        if (isBegin) {
            coupons.setIssueBeginTime(c.getIssueBeginTime());
            coupons.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupons);
        }

        // 6. 生成兑换码（若为兑换码类型且状态为待发放）
        if (coupons.getObtainWay() == 2 && coupons.getStatus() == 1) {
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

            // discountType 赋值（转换为枚举类型）
            vo.setDiscountType(DiscountType.of(c.getDiscountType()));

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

    /**
     * 多条件分页查询优惠券
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<CouponsPageVO> searchCoupons(CouponsSearchDTO queryDTO) {
        // 创建分页对象
        PageDTO<Coupons> page = new PageDTO<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<Coupons> queryWrapper = new LambdaQueryWrapper<>();
        // 过滤已删除的优惠券
        queryWrapper.eq(Coupons::getIsDelete, 0);

        // 折扣类型筛选
        if (queryDTO.getDiscountType() != null) {
            queryWrapper.eq(Coupons::getDiscountType, queryDTO.getDiscountType());
        }

        // 状态筛选
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(Coupons::getStatus, queryDTO.getStatus());
        }

        // 名称模糊搜索
        if (queryDTO.getName() != null && !queryDTO.getName().trim().isEmpty()) {
            queryWrapper.like(Coupons::getName, queryDTO.getName());
        }

        // 按创建时间降序排序
        queryWrapper.orderByDesc(Coupons::getCreateTime);

        // 执行分页查询
        PageDTO<Coupons> couponsPage = couponsMapper.selectPage(page, queryWrapper);

        // 转换为VO对象
        List<CouponsPageVO> records = couponsPage.getRecords().stream()
                .map(this::convertToPageVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        PageDTO<CouponsPageVO> resultPage = new PageDTO<>();
        resultPage.setRecords(records);
        resultPage.setTotal(couponsPage.getTotal());
        resultPage.setSize(couponsPage.getSize());
        resultPage.setCurrent(couponsPage.getCurrent());
        resultPage.setPages(couponsPage.getPages());

        return resultPage;
    }

    private CouponsPageVO convertToPageVO(Coupons coupons) {
        CouponsPageVO vo = new CouponsPageVO();
        // 其他属性赋值...
        vo.setId(coupons.getId());
        vo.setName(coupons.getName());
        vo.setSpecific(coupons.getSpecific());
        vo.setThresholdAmount(coupons.getThresholdAmount());
        vo.setDiscountValue(coupons.getDiscountValue());
        // VO 类保持 Integer，但赋值时处理小数
        if (coupons.getMaxDiscountAmount() != null) {
            // 乘以 100 转为分，再四舍五入避免截断
            vo.setMaxDiscountAmount(coupons.getMaxDiscountAmount()
                    .multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP) // 四舍五入
                    .intValue());
        } else {
            vo.setMaxDiscountAmount(0);
        }
        vo.setUsedNum(coupons.getUsedNum());
        vo.setIssueNum(coupons.getIssueNum());
        vo.setTotalNum(coupons.getTotalNum());
        vo.setCreateTime(coupons.getCreateTime());
        vo.setIssueBeginTime(coupons.getIssueBeginTime());
        vo.setIssueEndTime(coupons.getIssueEndTime());
        vo.setTermDays(coupons.getTermDays());
        vo.setTermBeginTime(coupons.getTermBeginTime());
        vo.setTermEndTime(coupons.getTermEndTime());

        // 关键修正：将整数转换为枚举对象
        vo.setDiscountType(DiscountType.of(coupons.getDiscountType())); // 折扣类型转换
        vo.setStatus(CouponStatus.of(coupons.getStatus())); // 状态转换

        // 处理获取方式（若需要，同样转换为ObtainType枚举）
        vo.setObtainWay(ObtainType.of(coupons.getObtainWay()));

        return vo;
    }

    /**
     * 每分钟执行一次：扫描未使用且已过期的优惠券，设置状态为3（已失效）
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟第0秒执行
    public void checkCouponExpire() {
        // 查询条件：
        // 1. status = 1（未使用）
        // 2. term_end_time < 当前时间
        List<UserCoupon> expiredCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getStatus, 1) // 未使用
                .lt(UserCoupon::getTermEndTime, new Date()) // 已过期
                .list();

        // 批量更新状态为3（已失效）
        if (!expiredCoupons.isEmpty()) {
            expiredCoupons.forEach(coupon -> {
                coupon.setStatus(UserCouponStatus.EXPIRED); // 设置为已失效
                coupon.setUpdateTime(new Date()); // 更新时间
            });
            // 批量更新（高效）
            userCouponService.updateBatchById(expiredCoupons);
            log.info("扫描到 {} 张过期优惠券，已设置为已失效状态", expiredCoupons.size());
        } else {
            log.info("未发现过期优惠券");
        }
    }

    /**
     * 每分钟执行一次：扫描优惠券发放结束时间是否过期，设置状态为“已结束”
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟第 0 秒执行
    @Transactional
    public void checkCouponIssueExpire() {
        // 查询条件：
        // 1. status 为“进行中”（状态 3）
        // 2. issue_end_time < 当前时间
        List<Coupons> expiredCoupons = lambdaQuery()
                .eq(Coupons::getStatus, CouponStatus.ISSUING) // 进行中状态
                .lt(Coupons::getIssueEndTime, new Date())      // 发放结束时间已过期
                .list();

        // 批量更新状态为“已结束”（状态 4）
        if (!expiredCoupons.isEmpty()) {
            expiredCoupons.forEach(coupon -> {
                coupon.setStatus(4); // 设置为已结束状态
                coupon.setUpdateTime(new Date());           // 更新时间
            });
            // 批量更新（高效）
            updateBatchById(expiredCoupons);
            log.info("扫描到 {} 张优惠券发放已过期，已设置为已结束状态", expiredCoupons.size());
        } else {
            log.info("未发现发放已过期的优惠券");
        }
    }

    // 已有的其他方法...
}