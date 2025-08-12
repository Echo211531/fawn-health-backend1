package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.user.AdminAddDTO;
import com.ljh.fawnhealth.model.dto.user.AdminUpdateDTO;
import com.ljh.fawnhealth.model.dto.user.UserPageQueryDTO;
import com.ljh.fawnhealth.model.dto.user.UserUpdateDTO;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.user.*;
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

    /**
     * 分页查询用户列表（支持多条件筛选）
     * 适用于管理员后台查询用户数据
     *
     * @param queryDTO 分页及查询条件
     * @return 分页结果（包含用户列表及分页信息）
     */
    IPage<UserVO> pageQueryUsers(UserPageQueryDTO queryDTO);

    /**
     * 启用/禁用用户账号
     * 仅管理员可操作，支持批量处理（单个ID或多个ID用逗号分隔）
     *
     * @param userIds  用户ID列表（单个ID或多个ID用逗号分隔，如"1,2,3"）
     * @param status   目标状态（0-禁用，1-启用）
     * @param id  请求对象（用于权限校验）
     * @return 操作结果
     */
    void updateUserStatus(String userIds, Integer status, Long id);

    /**
     * 添加管理员账号
     * 仅超级管理员可操作，默认角色为admin，密码默认123456（MD5加密）
     *
     * @param adminAddDTO 管理员信息（用户名、性别、邮箱）
     * @return 添加结果
     */
    void addAdmin(AdminAddDTO adminAddDTO,Long id);

    /**
     * 管理员修改个人信息接口
     *
     * @param adminId 管理员ID（路径参数，用于定位修改对象）
     * @param adminUpdateDTO 管理员信息更新参数
     * @return 更新后的管理员信息
     */
    User updateAdminInfo(Long adminId, AdminUpdateDTO adminUpdateDTO);

    /**
     * 获取用户性别分布统计数据
     * @return 性别分布统计VO
     */
    UserGenderStatisticsVO getGenderDistributionStatistics();
}
