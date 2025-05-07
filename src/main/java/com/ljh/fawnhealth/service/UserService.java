package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;


public interface UserService extends IService<User> {

    /**
     * 邮箱验证码登录
     * @param email
     * @param clientIp
     * @return
     */
    UserLoginVO findUserByEmail(String email, String clientIp);
}
