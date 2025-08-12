package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.mapper.UserLoginLogMapper;
import com.ljh.fawnhealth.model.entity.UserLoginLog;
import com.ljh.fawnhealth.service.UserLoginLogService;
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




