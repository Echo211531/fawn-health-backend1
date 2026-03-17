package com.zr.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zr.health.mapper.UserLoginLogMapper;
import com.zr.health.model.entity.UserLoginLog;
import com.zr.health.service.UserLoginLogService;
import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【user_login_log(用户登录日志表)】的数据库操作Service实现
* @createDate 2025-08-11 09:47:34
*/
@Service
public class UserLoginLogServiceImpl extends ServiceImpl<UserLoginLogMapper, UserLoginLog>
    implements UserLoginLogService {

}




