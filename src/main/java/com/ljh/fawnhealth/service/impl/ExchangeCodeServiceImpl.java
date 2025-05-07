package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.mapper.ExchangeCodeMapper;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.model.entity.ExchangeCode;
import com.ljh.fawnhealth.service.ExchangeCodeService;
import com.ljh.fawnhealth.utils.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.ljh.fawnhealth.constant.PromotionConstants.COUPON_CODE_MAP_KEY;
import static com.ljh.fawnhealth.constant.PromotionConstants.COUPON_CODE_SERIAL_KEY;

/**
* @author 27105
* @description 针对表【exchange_code(兑换码表)】的数据库操作Service实现
* @createDate 2025-05-02 23:03:33
*/
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode>
    implements ExchangeCodeService {

    private final StringRedisTemplate redisTemplate;
    private final BoundValueOperations<String, String> serialOps;

    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps = redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }

    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupons coupons) {
        // 发放数量
        Integer totalNum = coupons.getTotalNum();
        // 1.获取Redis自增序列号
        Long result = serialOps.increment(totalNum);
        if (result == null) {
            return;
        }
        int maxSerialNum = result.intValue();
        List<ExchangeCode> list = new ArrayList<>(totalNum);
        for (int serialNum = maxSerialNum - totalNum + 1; serialNum <= maxSerialNum; serialNum++) {
            // 2.生成兑换码
            String code = CodeUtil.generateCode(serialNum, coupons.getId());
            ExchangeCode e = new ExchangeCode();
            e.setCode(code);
            e.setId((long) serialNum);
            e.setExchangeTargetId(coupons.getId());
            e.setExpiredTime(coupons.getIssueEndTime());
            list.add(e);
        }
        // 3.保存数据库
        //saveBatch(list);
        for (ExchangeCode code : list) {
            this.save(code); // 通常是 BaseMapper 或 Service 中的 save 方法
        }

        // 4.写入Redis缓存，member：couponId，score：兑换码的最大序列号
        //redisTemplate.opsForZSet().add(COUPON_RANGE_KEY, coupons.getId().toString(), maxSerialNum);
    }

    @Override
    public boolean updateExchangeMark(long serialNum, boolean mark) {
        Boolean boo = redisTemplate.opsForValue().setBit(COUPON_CODE_MAP_KEY, serialNum, mark);
        return boo != null && boo;
    }
}




