package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.entity.UserFollow;

import java.util.List;

/**
 * @author ljh
 * @description 针对表【user_follow(用户关注表)】的数据库操作Service
 */
public interface UserFollowService extends IService<UserFollow> {
    
    /**
     * 关注/取消关注用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 新的关注状态（true=已关注，false=未关注）
     */
    boolean toggleFollow(Long followerId, Long followingId);
    
    /**
     * 检查是否已关注
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否已关注
     */
    boolean isFollowing(Long followerId, Long followingId);
    
    /**
     * 获取用户的关注列表（关注的博主）
     * @param followerId 关注者ID
     * @return 被关注者ID列表
     */
    List<Long> getFollowingList(Long followerId);
    
    /**
     * 获取用户的粉丝列表
     * @param followingId 被关注者ID
     * @return 关注者ID列表
     */
    List<Long> getFollowerList(Long followingId);
}

