package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.mapper.ExchangeCodeMapper;
import com.zr.health.model.dto.coupons.ExchangeCodeQueryDTO;
import com.zr.health.model.entity.Coupons;
import com.zr.health.model.entity.ExchangeCode;
import com.zr.health.model.vo.coupons.ExchangeCodeVO;
import com.zr.health.service.ExchangeCodeService;
import com.zr.health.utils.CodeUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zr.health.constant.PromotionConstants.COUPON_CODE_MAP_KEY;
import static com.zr.health.constant.PromotionConstants.COUPON_CODE_SERIAL_KEY;

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

    @Resource
    private ExchangeCodeMapper exchangeCodeMapper;

    /**
     * 异步生成优惠券兑换码
     * 1. 从Redis获取全局自增序列号作为基础
     * 2. 批量生成固定长度兑换码并关联优惠券
     * 3. 持久化到数据库
     * 4. 更新Redis中优惠券的最大序列号
     *
     * @param coupons 优惠券实体
     */
    @Override
    @Async("generateExchangeCodeExecutor") // 使用独立线程池异步执行
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

    /**
     * 分页查询兑换码
     *
     * @param queryDTO 查询参数（含分页、状态、优惠券ID）
     * @return 分页结果
     */
    @Override
    public PageDTO<ExchangeCodeVO> queryExchangeCodePage(ExchangeCodeQueryDTO queryDTO) {
        // 1. 构建分页参数（页码、每页条数）
        Page<ExchangeCode> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构建查询条件（使用LambdaQueryWrapper）
        LambdaQueryWrapper<ExchangeCode> queryWrapper = new LambdaQueryWrapper<>();
        // 隐含条件：必须关联指定优惠券ID
        queryWrapper.eq(ExchangeCode::getExchangeTargetId, queryDTO.getCouponId());
        // 可选条件：兑换码状态（如果传入了状态则过滤）
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(ExchangeCode::getStatus, queryDTO.getStatus());
        }
        // 排序：按兑换码ID倒序（最新生成的在前）
        queryWrapper.orderByDesc(ExchangeCode::getId);

        // 3. 调用MyBatis-Plus内置的selectPage方法（自动分页，无需写SQL）
        Page<ExchangeCode> exchangeCodePage = this.baseMapper.selectPage(page, queryWrapper);

        // 4. 转换为VO（实体 -> VO）
        PageDTO<ExchangeCodeVO> resultPage = new PageDTO<>();
        // 复制分页信息（总条数、总页数等）
        resultPage.setTotal(exchangeCodePage.getTotal());
        resultPage.setSize(exchangeCodePage.getSize());
        resultPage.setCurrent(exchangeCodePage.getCurrent());

        // 转换数据列表
        List<ExchangeCodeVO> voList = exchangeCodePage.getRecords().stream()
                .map(exchangeCode -> {
                    ExchangeCodeVO vo = new ExchangeCodeVO();
                    vo.setId(exchangeCode.getId());
                    vo.setCode(exchangeCode.getCode());
                    vo.setStatus(exchangeCode.getStatus());
                    vo.setExpiredTime(exchangeCode.getExpiredTime() != null ?
                            exchangeCode.getExpiredTime().toString() : null); // 时间格式化可按需调整
                    return vo;
                })
                .collect(Collectors.toList());
        resultPage.setRecords(voList);

        return resultPage;
    }

    /**
     * 实体转VO
     */
    private ExchangeCodeVO convertToVO(ExchangeCode exchangeCode) {
        ExchangeCodeVO vo = new ExchangeCodeVO();
        BeanUtils.copyProperties(exchangeCode, vo);
        // 如需格式化时间等额外处理，在这里添加
        return vo;
    }

}




