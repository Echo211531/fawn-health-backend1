package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.config.JwtProperties;
import com.ljh.fawnhealth.constant.JwtClaimsConstant;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.enums.user.UserRole;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtProperties jwtProperties; // 用于读取JWT配置（需配置此Bean）


    /**
     * 邮箱验证码登录
     * @param email
     * @param loginIp
     * @return
     */
    @Override
    public UserLoginVO findUserByEmail(String email, String loginIp) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setEmailVerified(0);
            user.setNickname("小鹿友友" + UUID.randomUUID().toString().substring(0, 6));
            user.setStatus(1);
            user.setIsVip(0);
            user.setGender(0);
            user.setRole(UserRole.USER.getDescription());
            user.setAvatar("http://fawn-health.oss-cn-chengdu.aliyuncs.com/fawn-health-userAvatar.png");
            user.setCreateTime(new Date());
        }

        // 每次登录都更新以下字段
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(loginIp);
        user.setUpdateTime(new Date());

        // 插入或更新数据库
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }

        // 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );

        // 构建返回对象
        UserLoginVO vo = new UserLoginVO();
        BeanUtils.copyProperties(user, vo);
        vo.setToken(token);

        return vo;
    }

}




