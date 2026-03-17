package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zr.health.exception.ThrowUtils;
import com.zr.health.exception.ErrorCode;
import com.zr.health.mapper.UserFollowMapper;
import com.zr.health.model.entity.UserFollow;
import com.zr.health.service.UserFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ljh
 * @description 针对表【user_follow(用户关注表)】的数据库操作Service实现
 */
@Slf4j
@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow>
        implements UserFollowService {

    @Override
    @Transactional
    public boolean toggleFollow(Long followerId, Long followingId) {
        // 参数校验
        ThrowUtils.throwIf(followerId == null, ErrorCode.PARAMS_ERROR, "关注者ID不能为空");
        ThrowUtils.throwIf(followingId == null, ErrorCode.PARAMS_ERROR, "被关注者ID不能为空");
        ThrowUtils.throwIf(followerId.equals(followingId), ErrorCode.PARAMS_ERROR, "不能关注自己");

        // 查询是否已关注
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("follower_id", followerId)
                .eq("following_id", followingId)
                .eq("is_delete", 0);
        UserFollow existingFollow = this.getOne(queryWrapper);

        boolean newFollowState;
        if (existingFollow != null) {
            // 已关注，执行取消关注（软删除）
            existingFollow.setIsDelete(1);
            existingFollow.setUpdateTime(new Date());
            this.updateById(existingFollow);
            newFollowState = false;
            log.info("用户{}取消关注用户{}", followerId, followingId);
        } else {
            // 未关注，执行关注操作
            // 先检查是否有历史记录（已取消关注的）
            QueryWrapper<UserFollow> historyQuery = new QueryWrapper<>();
            historyQuery.eq("follower_id", followerId)
                    .eq("following_id", followingId);
            UserFollow historyFollow = this.getOne(historyQuery);

            if (historyFollow != null) {
                // 恢复关注
                historyFollow.setIsDelete(0);
                historyFollow.setUpdateTime(new Date());
                this.updateById(historyFollow);
            } else {
                // 新增关注记录
                UserFollow follow = new UserFollow();
                follow.setFollowerId(followerId);
                follow.setFollowingId(followingId);
                follow.setIsDelete(0);
                follow.setCreateTime(new Date());
                follow.setUpdateTime(new Date());
                this.save(follow);
            }
            newFollowState = true;
            log.info("用户{}关注用户{}", followerId, followingId);
        }

        return newFollowState;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            return false;
        }
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("follower_id", followerId)
                .eq("following_id", followingId)
                .eq("is_delete", 0);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<Long> getFollowingList(Long followerId) {
        if (followerId == null) {
            return List.of();
        }
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("follower_id", followerId)
                .eq("is_delete", 0);
        List<UserFollow> follows = this.list(queryWrapper);
        return follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getFollowerList(Long followingId) {
        if (followingId == null) {
            return List.of();
        }
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("following_id", followingId)
                .eq("is_delete", 0);
        List<UserFollow> follows = this.list(queryWrapper);
        return follows.stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());
    }
}

