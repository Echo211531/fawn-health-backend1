package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    @Select("select id from user")
    List<Long> selectActiveUserIds();
}




