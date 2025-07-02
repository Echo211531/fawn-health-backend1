package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;


public interface UserService extends IService<User> {

    /**
     * 邮箱验证码登录
     * @param email
     * @param clientIp
     * @return
     */
    UserLoginVO findUserByEmail(String email, String clientIp,HttpServletRequest request);

    /**
     * 更新用户体重和目标体重信息
     * @param userId 用户ID
     * @param weight 体重(kg)
     * @param targetWeight 目标体重(kg)
     * @return 更新是否成功
     */
    void updateWeightInfo(Long userId, BigDecimal weight, BigDecimal targetWeight, Integer periodDays);
    User getLoginUser(HttpServletRequest request);
}
