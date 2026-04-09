package com.zr.health.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.constant.PromotionConstants;
import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.mapper.CouponsMapper;
import com.zr.health.mapper.UserCouponMapper;
import com.zr.health.model.dto.coupons.UserCouponDTO;
import com.zr.health.model.dto.order.CouponDiscountDTO;
import com.zr.health.model.dto.order.OrderProductDTO;
import com.zr.health.model.entity.Coupons;
import com.zr.health.model.entity.ExchangeCode;
import com.zr.health.model.entity.UserCoupon;
import com.zr.health.model.enums.coupons.DiscountType;
import com.zr.health.model.enums.coupons.ExchangeCodeStatus;
import com.zr.health.model.enums.coupons.UserCouponStatus;
import com.zr.health.model.vo.coupons.UserCouponsVO;
import com.zr.health.mq.MessageProducer;
import com.zr.health.mq.MqConstant;
import com.zr.health.service.ExchangeCodeService;
import com.zr.health.service.UserCouponService;
import com.zr.health.strategy.discount.Discount;
import com.zr.health.strategy.discount.DiscountStrategy;
import com.zr.health.utils.BeanCopyUtils;
import com.zr.health.utils.CodeUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.bean.BeanUtil;


import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
* @author 27105
* @description 针对表【user_coupon(用户领取优惠券的记录，是真正使用的优惠券信息)】的数据库操作Service实现
* @createDate 2025-05-03 19:36:42
*/
@Slf4j
@Service
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon>
    implements UserCouponService {

    /**
     * Redis Lua 脚本：原子预扣减优惠券库存 + 校验并更新用户限领次数
     *
     * KEYS[1] = 库存 key，例如 prs:coupon:{couponId}:stock
     * KEYS[2] = 用户限领 hash key，例如 prs:user:coupon:{couponId}
     * ARGV[1] = userId
     * ARGV[2] = userLimit
     *
     * 返回值：
     *  1  成功预扣减
     *  0  库存不足
     * -1 超出用户限领
     */
    private static final String COUPON_PRE_DECR_LUA =
            "local stock = redis.call('get', KEYS[1])\n" +
            "if not stock then\n" +
            "    return 0\n" +
            "end\n" +
            "local stockNum = tonumber(stock)\n" +
            "if not stockNum or stockNum <= 0 then\n" +
            "    return 0\n" +
            "end\n" +
            "stockNum = stockNum - 1\n" +
            "if stockNum < 0 then\n" +
            "    return 0\n" +
            "end\n" +
            "redis.call('set', KEYS[1], tostring(stockNum))\n" +
            "\n" +
            "local userId = ARGV[1]\n" +
            "local userLimit = tonumber(ARGV[2])\n" +
            "\n" +
            "local userCount = redis.call('hget', KEYS[2], userId)\n" +
            "if not userCount then\n" +
            "    userCount = 0\n" +
            "else\n" +
            "    userCount = tonumber(userCount)\n" +
            "    if not userCount then\n" +
            "        userCount = 0\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "userCount = userCount + 1\n" +
            "if userCount > userLimit then\n" +
            "    redis.call('set', KEYS[1], tostring(stockNum + 1))\n" +
            "    return -1\n" +
            "end\n" +
            "\n" +
            "redis.call('hset', KEYS[2], userId, tostring(userCount))\n" +
            "return 1\n";

    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private CouponsMapper couponsMapper;

    @Resource
    private ExchangeCodeService exchangeCodeService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessageProducer messageProducer;
//    /**
//     * 领取优惠券
//     *
//     * @param couponsId
//     * @return
//     */
//    @Override
//    @Transactional
//    public int receiveCoupon(Long couponsId,Long userId) {
//        // 1.查询优惠券
//        Coupons coupons = couponsMapper.selectById(couponsId);
//        if (coupons == null) {
//            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
//        }
//        // 2.校验发放时间
//        Date now = new Date();
//        if (now.before(coupons.getIssueBeginTime()) || now.after(coupons.getIssueEndTime())) {
//            throw new BusinessException(ErrorCode.COUPON_BEGIN_END);
//        }
//        // 3.校验库存
//        if (coupons.getIssueNum() >= coupons.getTotalNum()) {
//            throw new BusinessException(ErrorCode.COUPON_STOCK);
//        }
//        String key = "lock:coupons:uid:" + userId;
//        RLock lock = redissonClient.getLock(key);
//        boolean isLock = lock.tryLock();
//        if (!isLock) {
//            throw new BusinessException(ErrorCode.REQUEST_ARE_FREQUENT);
//        }
//        try {
//            UserCouponService userCouponService = (UserCouponService) AopContext.currentProxy();
//            int i = userCouponService.checkAndCreateUserCoupon(coupons, userId, null);
//            return i;
//        }finally {
//            lock.unlock();
//        }
//        synchronized (userId.toString().intern()) {
//            // 4.校验每人限领数量
//            // 4.1.统计当前用户对当前优惠券的已经领取的数量
//            Integer count = Math.toIntExact(lambdaQuery()
//                    .eq(UserCoupon::getUserId, userId)
//                    .eq(UserCoupon::getCouponId, couponsId)
//                    .count());
//            // 4.2.校验限领数量
//            if (count != null && count >= coupons.getUserLimit()) {
//                throw new BusinessException(ErrorCode.COUPON_OVER_LIMIT);
//            }
//            // 5.更新优惠券的已经发放的数量 + 1
//            int i1 = couponsMapper.incrIssueNum(coupons.getId());
//
//            if (i1 == 0) {
//                throw new BusinessException(ErrorCode.COUPON_OVER_LIMIT);
//            }
//            // 6.新增一个用户券
//            int i = saveUserCoupon(coupons, userId);
//            return i;
//             UserCouponService userCouponService = (UserCouponService) AopContext.currentProxy();
//             int i = userCouponService.checkAndCreateUserCoupon(coupons, userId, null);
//             return i;
//        }
//    }

    /**
     * 领取优惠券（Lua + Redis 原子预扣减 + MQ 最终一致性）
     *
     * @param couponsId 优惠券ID
     * @param userId    用户ID
     * @return 1 表示预扣成功
     */
    @Override
    public int receiveCoupon(Long couponsId, Long userId) {
        // 1. 查询优惠券
        Coupons coupons = couponsMapper.selectById(couponsId);
        if (coupons == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }

        // 2. 校验发放时间
        Date now = new Date();
        if (now.before(coupons.getIssueBeginTime()) || now.after(coupons.getIssueEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_BEGIN_END);
        }

        // 3. 准备 Redis key
        String stockKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponsId + ":stock";
        String userKey = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponsId;

        // 3.1 冷启动时惰性初始化库存到 Redis（非强一致，但只影响首次）
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
            int remainingStock = coupons.getTotalNum() - coupons.getIssueNum();
            if (remainingStock <= 0) {
                throw new BusinessException(ErrorCode.COUPON_STOCK);
            }
            // 强制转换为纯数字字符串，避免格式问题
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(remainingStock).trim());
            log.info("初始化优惠券库存 - stockKey: {}, 库存值: {}", stockKey, remainingStock);
        }
        // 3. 执行脚本前，先读取Redis中的库存值（确认代码能读到正确值）
        String redisStock = stringRedisTemplate.opsForValue().get(stockKey);
        log.info("【领券】代码读取的库存值 - stockKey: {}, value: {}, 类型: {}",
                stockKey, redisStock, (redisStock == null ? "null" : redisStock.getClass().getName()));
        // 4. 执行 Lua：原子预扣减库存 + 用户限领计数
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(COUPON_PRE_DECR_LUA);
        script.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(
                script,
                Arrays.asList(stockKey, userKey),
                userId.toString(),
                String.valueOf(coupons.getUserLimit())
        );

        if (result == null || result == 0L) {
            // 库存不足
            throw new BusinessException(ErrorCode.COUPON_STOCK);
        }
        if (result == -1L) {
            // 超出限领
            throw new BusinessException(ErrorCode.COUPON_OVER_LIMIT);
        }

        String requestId = UUID.randomUUID().toString();
        UserCouponDTO uc = new UserCouponDTO();
        uc.setUserId(userId);
        uc.setCouponId(couponsId);
        uc.setRequestId(requestId);
        messageProducer.sendCouponMessage(MqConstant.FH_ROUTING_KEY, uc);

        log.info("优惠券 Lua 预扣减成功: userId={}, couponId={}, requestId={}", userId, couponsId, requestId);
        // 用户券记录由 MQ 异步创建，这里快速返回
        return 1;
    }


    private Coupons queryCouponByCache(Long couponId) {
        // 1.准备KEY
        String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId;
        // 2.查询
        Map<Object, Object> objMap = stringRedisTemplate.opsForHash().entries(key);
        System.out.println("Redis 中缓存的 coupon 数据：" + objMap);
        if (objMap.isEmpty()) {
            return null;
        }
        // 3.数据反序列化
        return BeanUtil.mapToBean(objMap, Coupons.class, false, CopyOptions.create());
    }

    private int saveUserCoupon(Coupons coupons, Long userId) {
        // 1.基本信息
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(coupons.getId());
        uc.setStatus(UserCouponStatus.UNUSED);
        System.out.println("Coupon ID: " + uc.getCouponId());
        // 2.有效期信息
        Date termBeginTime = coupons.getTermBeginTime();
        Date termEndTime = coupons.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = new Date();
            termEndTime = DateUtils.addDays(termBeginTime, coupons.getTermDays());
        }
        uc.setTermBeginTime(termBeginTime);
        uc.setTermEndTime(termEndTime);

        // 3.保存
        return userCouponMapper.insert(uc);
    }

    /**
     * 校验并创建用户券（带延迟双删机制，保证缓存一致性）
     * @param coupons
     * @param userId
     * @param serialNum
     * @return
     */
    @Override
    @Transactional
    public int checkAndCreateUserCoupon(Coupons coupons, Long userId, Integer serialNum){
            int count = Math.toIntExact(lambdaQuery()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponId, coupons.getId())
                    .count());
            if (count >= coupons.getUserLimit()) {
                throw new BusinessException(ErrorCode.COUPON_OVER_LIMIT);
            }

            int updateCount = couponsMapper.incrIssueNum(coupons.getId());
            if (updateCount == 0) {
                throw new BusinessException(ErrorCode.COUPON_STOCK);
            }

            int i = saveUserCoupon(coupons, userId);

            if (serialNum != null) {
                exchangeCodeService.lambdaUpdate()
                        .set(ExchangeCode::getUserId, userId)
                        .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                        .eq(ExchangeCode::getId, serialNum)
                        .update();
            }
            return i;

    }

    /**
     * 兑换码兑换优惠券
     *
     * @param userId
     * @param code
     * @return
     */
    @Override
    @Transactional
    public void exchangeCoupon(Long userId, String code) {
        // 1.校验并解析兑换码
        long serialNum = CodeUtil.parseCode(code);
        // 2.校验是否已经兑换 SETBIT KEY 4 1 ，这里直接执行setbit，通过返回值来判断是否兑换过
        boolean exchanged = exchangeCodeService.updateExchangeMark(serialNum, true);
        if (exchanged) {
            throw new BusinessException(ErrorCode.EXCHANGE_CODE_USE);
        }
        try {
            // 3.查询兑换码对应的优惠券id
            ExchangeCode exchangeCode = exchangeCodeService.getById(serialNum);
            if (exchangeCode == null) {
                throw new BusinessException(ErrorCode.EXCHANGE_CODE_NOT_FOUND);
            }
            // 4.是否过期
            Date now = new Date();
            if (now.after(exchangeCode.getExpiredTime())){
                throw new BusinessException(ErrorCode.EXCHANGE_CODE_OVERDUE);
            }
            // 5.校验并生成用户券
            // 5.1.查询优惠券
            Coupons coupons = couponsMapper.selectById(exchangeCode.getExchangeTargetId());
            // 新增：检查优惠券是否为暂停状态（假设暂停状态值为5，根据实际枚举值修改）
            if (coupons.getStatus() == 5) { // 这里的5需要替换为实际的"暂停"状态枚举值
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "优惠券已暂停，无法兑换");
            }
            // 5.2.校验并生成用户券，更新兑换码状态
            checkAndCreateUserCoupon(coupons, userId, (int) serialNum);
        } catch (Exception e) {
            // 重置兑换的标记 0
            exchangeCodeService.updateExchangeMark(serialNum, false);
            throw e;
        }
    }

    public List<UserCouponsVO> listUserCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(UserCoupon::getStatus, UserCouponStatus.of(status));
        }
        queryWrapper.orderByDesc(UserCoupon::getCreateTime);

        List<UserCoupon> userCoupons = userCouponMapper.selectList(queryWrapper);

        // 获取所有优惠券ID
        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .distinct()
                .collect(Collectors.toList());

        List<Coupons> coupons;
        Map<Long, Coupons> couponMap;

        // 判断couponIds是否为空，避免传空集合给selectBatchIds
        if (couponIds.isEmpty()) {
            coupons = Collections.emptyList();
            couponMap = Collections.emptyMap();
        } else {
            coupons = couponsMapper.selectBatchIds(couponIds);
            couponMap = coupons.stream()
                    .collect(Collectors.toMap(Coupons::getId, c -> c));
        }

        return userCoupons.stream()
                .map(userCoupon -> {
                    UserCouponsVO vo = new UserCouponsVO();
                    BeanCopyUtils.copy(userCoupon, vo);

                    // 设置状态枚举转int
                    if (userCoupon.getStatus() != null) {
                        vo.setStatus(userCoupon.getStatus().getValue());
                    }

                    Coupons coupons1 = couponMap.get(userCoupon.getCouponId());
                    if (coupons1 != null) {
                        vo.setCouponName(coupons1.getName());
                        vo.setDiscountType(coupons1.getDiscountType());
                        vo.setThresholdAmount(coupons1.getThresholdAmount());
                        vo.setDiscountValue(coupons1.getDiscountValue());
                        vo.setMaxDiscountAmount(coupons1.getMaxDiscountAmount());
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 返回可用优惠券信息
     *
     * @param orderProducts
     * @return
     */
//    @Override
//    public List<CouponDiscountDTO> findDiscountSolution(List<OrderProductDTO> orderProducts, Long userId) {
//        List<Coupons> coupons = userCouponMapper.querMyCoupons(userId);
//        if(coupons == null || coupons.isEmpty()){
//            return Collections.emptyList();
//        }
//
//        int totalAmount = orderProducts.stream().mapToInt(OrderProductDTO::getPrice).sum();
//        List<Coupons> availableCoupons = coupons.stream()
//                .filter(c -> DiscountStrategy.getDiscount(DiscountType.of(c.getDiscountType())).canUse(totalAmount, c))
//                .collect(Collectors.toList());
//
//        if(availableCoupons == null || availableCoupons.isEmpty()){
//            return Collections.emptyList();
//        }
//
//        // 3.排列组合出所有方案
//        // 3.1.细筛（找出每一个优惠券的可用的课程，判断课程总价是否达到优惠券的使用需求）
//        Map<Coupons, List<OrderProductDTO>> availableCouponMap = findAvailableCoupons(availableCoupons, orderProducts);
//        if(availableCouponMap == null || availableCouponMap.isEmpty()){
//            return Collections.emptyList();
//        }
//        // 3.2.排列组合
//        availableCoupons = new ArrayList<>(availableCouponMap.keySet());
//        List<List<Coupons>> solutions = PermuteUtil.permute(availableCoupons);
//        // 3.3.添加单券的方案
//        for (Coupons c : availableCoupons) {
//            solutions.add(List.of(c));
//        }
//
//        // 4.计算方案的优惠明细
//        List<CouponDiscountDTO> list =
//                Collections.synchronizedList(new ArrayList<>(solutions.size()));
//        for (List<Coupons> solution : solutions) {
//            list.add(calculateSolutionDiscount(availableCouponMap, orderProducts, solution));
//        }
//
//        // 5.筛选最优解
//        return findBestSolution(list);
//    }

    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderProductDTO> orderProducts, Long userId) {
        List<Coupons> coupons = userCouponMapper.querMyCoupons(userId);
        if(coupons == null || coupons.isEmpty()){
            return Collections.emptyList();
        }

        int totalAmount = orderProducts.stream().mapToInt(OrderProductDTO::getPrice).sum();
        List<Coupons> availableCoupons = coupons.stream()
                .filter(c -> DiscountStrategy.getDiscount(DiscountType.of(c.getDiscountType())).canUse(totalAmount, c))
                .collect(Collectors.toList());

        if(availableCoupons == null || availableCoupons.isEmpty()){
            return Collections.emptyList();
        }

        // 3.只保留单券方案（删除多券组合逻辑）
        // 3.1.细筛（找出每一个优惠券的可用的课程，判断课程总价是否达到优惠券的使用需求）
        Map<Coupons, List<OrderProductDTO>> availableCouponMap = findAvailableCoupons(availableCoupons, orderProducts);
        if(availableCouponMap == null || availableCouponMap.isEmpty()){
            return Collections.emptyList();
        }

        // 3.2.仅保留单券方案（删除多券排列组合）
        availableCoupons = new ArrayList<>(availableCouponMap.keySet());
        List<List<Coupons>> solutions = new ArrayList<>();
        // 只添加单券方案，不生成任何组合
        for (Coupons c : availableCoupons) {
            solutions.add(List.of(c));
        }

        // 4.计算方案的优惠明细
        List<CouponDiscountDTO> list =
                Collections.synchronizedList(new ArrayList<>(solutions.size()));
        for (List<Coupons> solution : solutions) {
            list.add(calculateSolutionDiscount(availableCouponMap, orderProducts, solution));
        }

        // 5.筛选最优解
        return findBestSolution(list);
    }


    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        return list.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed()
                        .thenComparing(dto -> dto.getIds().stream().min(Long::compareTo).orElse(Long.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    private CouponDiscountDTO calculateSolutionDiscount(Map<Coupons, List<OrderProductDTO>> availableCouponMap, List<OrderProductDTO> orderProducts, List<Coupons> solution) {
        CouponDiscountDTO dto = new CouponDiscountDTO();
        if (solution == null || solution.isEmpty()) {
            return dto;
        }
        Coupons coupon = solution.get(0);
        List<OrderProductDTO> availableCourses = availableCouponMap.get(coupon);
        if (availableCourses == null || availableCourses.isEmpty()) {
            return dto;
        }
        int totalAmount = availableCourses.stream().mapToInt(OrderProductDTO::getPrice).sum();
        Discount discount = DiscountStrategy.getDiscount(DiscountType.of(coupon.getDiscountType()));
        if (!discount.canUse(totalAmount, coupon)) {
            return dto;
        }
        int discountAmount = discount.calculateDiscount(totalAmount, coupon);
        if (discountAmount < 0) {
            discountAmount = 0;
        } else if (discountAmount > totalAmount) {
            discountAmount = totalAmount;
        }
        dto.getIds().add(coupon.getCreater());
        dto.getRules().add(discount.getRule(coupon));
        dto.setDiscountAmount(discountAmount);
        return dto;
    }

    private void calculateDiscountDetails(Map<Long, Integer> detailMap, List<OrderProductDTO> availableCourses, int totalAmount, int discountAmount) {
        int times = 0;
        int remainDiscount = discountAmount;
        for (OrderProductDTO course : availableCourses) {
            // 更新课程已计算数量
            times++;
            int discount = 0;
            // 判断是否是最后一个课程
            if (times == availableCourses.size()) {
                // 是最后一个课程，总折扣金额 - 之前所有商品的折扣金额之和
                discount = remainDiscount;
            } else {
                // 计算折扣明细（课程价格在总价中占的比例，乘以总的折扣）
                discount = discountAmount * course.getPrice() / totalAmount;
                remainDiscount -= discount;
            }
            // 更新折扣明细
            detailMap.put(course.getId(), discount + detailMap.get(course.getId()));
        }
    }

    private Map<Coupons, List<OrderProductDTO>> findAvailableCoupons(
            List<Coupons> coupons, List<OrderProductDTO> courses) {
        Map<Coupons, List<OrderProductDTO>> map = new HashMap<>(coupons.size());
        for (Coupons coupon : coupons) {
            // 1.找出优惠券的可用的商品
            List<OrderProductDTO> availableCourses = courses;
            if (coupon.getSpecific()) {
//                // 1.1.限定了范围，查询券的可用范围
//                List<CouponScope> scopes = scopeService.lambdaQuery().eq(CouponsScope::getCouponId, coupon.getId()).list();
//                // 1.2.获取范围对应的分类id
//                Set<Long> scopeIds = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
//                // 1.3.筛选课程
//                availableCourses = courses.stream()
//                        .filter(c -> scopeIds.contains(c.getCateId())).collect(Collectors.toList());
            }
            if(availableCourses.isEmpty()){
                // 没有任何可用课程，抛弃
                continue;
            }
            // 2.计算商品总价
            int totalAmount = availableCourses.stream().mapToInt(OrderProductDTO::getPrice).sum();
            // 3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(DiscountType.of(coupon.getDiscountType()));
            if (discount.canUse(totalAmount, coupon)) {
                map.put(coupon, availableCourses);
            }
        }
        return map;
    }


}




