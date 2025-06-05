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
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.model.entity.ExchangeCode;
import com.ljh.fawnhealth.model.entity.UserCoupon;
import com.ljh.fawnhealth.model.enums.coupons.ExchangeCodeStatus;
import com.ljh.fawnhealth.model.enums.coupons.UserCouponStatus;
import com.ljh.fawnhealth.model.vo.coupons.UserCouponsVO;
import com.ljh.fawnhealth.mq.MessageProducer;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.service.ExchangeCodeService;
import com.ljh.fawnhealth.service.UserCouponService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import com.ljh.fawnhealth.utils.CodeUtil;
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


import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        Coupons coupons = queryCouponByCache(couponsId);
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
                        vo.setMaxDiscountAmount(coupons1.getMaxDiscountAmount());
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }




}




