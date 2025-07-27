package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.user.UserUpdateDTO;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.user.UserLoginStatisticsVO;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import com.ljh.fawnhealth.model.vo.user.UserNewStatisticsVO;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;


public interface UserService extends IService<User> {

    /**
     * 邮箱验证码登录
     *
     * @param email
     * @param clientIp
     * @return
     */
    UserLoginVO findUserByEmail(String email, String clientIp,HttpServletRequest request);

    /**
     * 更新用户体重和目标体重信息
     *
     * @param userId 用户ID
     * @param weight 体重(kg)
     * @param targetWeight 目标体重(kg)
     * @return 更新是否成功
     */
    void updateWeightInfo(Long userId, BigDecimal weight, BigDecimal targetWeight, Integer periodDays);

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param userUpdateDTO 包含要更新的用户信息
     * @return 更新后的用户信息
     */
    User updateUserInfo(Long userId, UserUpdateDTO userUpdateDTO);

    /**
     * 批量获取用户信息
     *
     * @param userIds
     * @return
     */
    Map<Long, User> getUserMapByIds(Set<Long> userIds);

    /**
     * 管理员登录
     *
     * @param username
     * @param password
     * @return
     */
    UserLoginVO findAdminByEmail(String username, String password);

    /**
     * 获取用户新增数据统计（今日、昨日、本月及日环比）
     *
     * @return 统计结果VO
     */
    UserNewStatisticsVO getNewUsersStatistics();

    /**
     * 获取用户登录统计数据
     *
     * @return 登录统计结果VO
     */
    UserLoginStatisticsVO getLoginStatistics();
}
