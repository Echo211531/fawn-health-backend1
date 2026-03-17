package com.zr.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zr.health.model.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ljh
 * @description 针对表【user_follow(用户关注表)】的数据库操作Mapper
 * @createDate 2024-01-26
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

}

