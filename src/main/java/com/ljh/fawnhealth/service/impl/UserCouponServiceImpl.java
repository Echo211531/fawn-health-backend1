package com.ljh.fawnhealth.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.constant.PromotionConstants;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.CouponsMapper;
import com.ljh.fawnhealth.mapper.UserCouponMapper;
import com.ljh.fawnhealth.model.dto.coupons.UserCouponDTO;
import com.ljh.fawnhealth.model.dto.order.CouponDiscountDTO;
import com.ljh.fawnhealth.model.dto.order.OrderProductDTO;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.model.entity.ExchangeCode;
import com.ljh.fawnhealth.model.entity.UserCoupon;
import com.ljh.fawnhealth.model.enums.coupons.DiscountType;
import com.ljh.fawnhealth.model.enums.coupons.ExchangeCodeStatus;
import com.ljh.fawnhealth.model.enums.coupons.UserCouponStatus;
import com.ljh.fawnhealth.model.vo.coupons.UserCouponsVO;
import com.ljh.fawnhealth.mq.MessageProducer;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.service.ExchangeCodeService;
import com.ljh.fawnhealth.service.UserCouponService;
import com.ljh.fawnhealth.strategy.discount.Discount;
import com.ljh.fawnhealth.strategy.discount.DiscountStrategy;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import com.ljh.fawnhealth.utils.CodeUtil;
import com.ljh.fawnhealth.utils.PermuteUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;


import java.util.*;
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

    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private CouponsMapper couponsMapper;

    @Resource
    private ExchangeCodeService exchangeCodeService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String,String> redisTemplate;

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
     * 领取优惠券
     * @param couponsId
     * @param userId
     * @return
     */
    @Override
    public int receiveCoupon(Long couponsId, Long userId) {
        // 1.查询优惠券
        Coupons coupons = couponsMapper.selectById(couponsId);
        System.out.println("优惠券信息："+ coupons);
        if (coupons == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }

        // 2.校验发放时间
        Date now = new Date();
        if (now.before(coupons.getIssueBeginTime()) || now.after(coupons.getIssueEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_BEGIN_END);
        }
        // 3.校验库存
        System.out.println("优惠券领取数量："+ coupons.getIssueNum());
        if (coupons.getIssueNum() >= coupons.getTotalNum()) {
            throw new BusinessException(ErrorCode.COUPON_STOCK);
       }

        // 4.校验每人限领数量
        String key = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponsId;
        RLock lock = redissonClient.getLock(key);
        boolean isLock = lock.tryLock();

        if (!isLock) {
            throw new BusinessException(ErrorCode.REQUEST_ARE_FREQUENT);
        }

        try {
            // AOP 调用代理方法
            UserCouponService userCouponService = (UserCouponService) AopContext.currentProxy();
            int result = userCouponService.checkAndCreateUserCoupon(coupons, userId, null);

            // 4.1 增加用户领取数量（Redis中）
            Long count = redisTemplate.opsForHash().increment(key, userId.toString(), 1);
            if (count != null && count > coupons.getUserLimit()) {
                throw new BusinessException(ErrorCode.COUPON_OVER_LIMIT);
            }

            // 5. 扣减库存
//            redisTemplate.opsForHash().increment(
//                    PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponsId, "totalNum", -1);

            // 获取当前的 totalNum 值
            String value = (String) redisTemplate.opsForHash().get(PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponsId, "totalNum");
            if (value != null) {
                try {
                    Integer totalNum = Integer.parseInt(value);
                    // 进行操作
                } catch (NumberFormatException e) {
                    // 处理转换异常，日志记录或返回错误
                }
            }


            // 6. 发送 MQ 消息
            UserCouponDTO uc = new UserCouponDTO();
            uc.setUserId(userId);
            uc.setCouponId(couponsId);
            messageProducer.sendCouponMessage(MqConstant.FH_ROUTING_KEY, uc);

            return result;
        } finally {
            lock.unlock();
        }
    }


    private Coupons queryCouponByCache(Long couponId) {
        // 1.准备KEY
        String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId;
        // 2.查询
        Map<Object, Object> objMap = redisTemplate.opsForHash().entries(key);
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
        int i = userCouponMapper.insert(uc);
        return i;
    }

    @Override
    @Transactional
    public int checkAndCreateUserCoupon(Coupons coupons, Long userId, Integer serialNum){
        // 1.校验每人限领数量
        // 1.1.统计当前用户对当前优惠券的已经领取的数量
        Integer count = Math.toIntExact(lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupons.getId())
                .count());
        // 1.2.校验限领数量
        if(count != null && count >= coupons.getUserLimit()){
            throw new BusinessException(ErrorCode.COUPON_OVER_LIMIT);
        }
        // 2.更新优惠券的已经发放的数量 + 1
        couponsMapper.incrIssueNum(coupons.getId());
        // 3.新增一个用户券
        int i = saveUserCoupon(coupons, userId);
        // 4.更新兑换码状态
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
        // 1.准备Map记录最优解
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
        // 2.遍历，筛选最优解
        for (CouponDiscountDTO solution : list) {
            // 2.1.计算当前方案的id组合
            String ids = solution.getIds().stream()
                    .sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
            // 2.2.比较用券相同时，优惠金额是否最大
            CouponDiscountDTO best = moreDiscountMap.get(ids);
            if (best != null && best.getDiscountAmount() >= solution.getDiscountAmount()) {
                // 当前方案优惠金额少，跳过
                continue;
            }
            // 2.3.比较金额相同时，用券数量是否最少
            best = lessCouponMap.get(solution.getDiscountAmount());
            int size = solution.getIds().size();
            if (size > 1 && best != null && best.getIds().size() <= size) {
                // 当前方案用券更多，放弃
                continue;
            }
            // 2.4.更新最优解
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        // 3.求交集
        Collection<CouponDiscountDTO> moreDiscounts = moreDiscountMap.values();
        Collection<CouponDiscountDTO> lessCoupons = lessCouponMap.values();

        Collection<CouponDiscountDTO> bestSolutions = moreDiscounts.stream()
                .filter(lessCoupons::contains)
                .collect(Collectors.toList());

        // 4.排序，按优惠金额降序
        return bestSolutions.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }

    private CouponDiscountDTO calculateSolutionDiscount(Map<Coupons, List<OrderProductDTO>> availableCouponMap, List<OrderProductDTO> orderProducts, List<Coupons> solution) {
            // 1.初始化DTO
            CouponDiscountDTO dto = new CouponDiscountDTO();
            // 2.初始化折扣明细的映射
            Map<Long, Integer> detailMap = orderProducts.stream().collect(Collectors.toMap(OrderProductDTO::getId, oc -> 0));
            // 3.计算折扣
            for (Coupons coupon : solution) {
                // 3.1.获取优惠券限定范围对应的课程
                List<OrderProductDTO> availableCourses = availableCouponMap.get(coupon);
                // 3.2.计算课程总价(课程原价 - 折扣明细)
                int totalAmount = availableCourses.stream()
                        .mapToInt(oc -> oc.getPrice() - detailMap.get(oc.getId())).sum();
                // 3.3.判断是否可用
                Discount discount = DiscountStrategy.getDiscount(DiscountType.of(coupon.getDiscountType()));
                if (!discount.canUse(totalAmount, coupon)) {
                    // 券不可用，跳过
                    continue;
                }
                // 3.4.计算优惠金额
                int discountAmount = discount.calculateDiscount(totalAmount, coupon);
                // 3.5.计算优惠明细
                calculateDiscountDetails(detailMap, availableCourses, totalAmount, discountAmount);
                // 3.6.更新DTO数据
                dto.getIds().add(coupon.getCreater());
                dto.getRules().add(discount.getRule(coupon));
                dto.setDiscountAmount(discountAmount + dto.getDiscountAmount());
            }
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




