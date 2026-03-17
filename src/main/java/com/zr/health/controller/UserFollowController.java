package com.zr.health.controller;

import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.model.entity.User;
import com.zr.health.model.vo.user.UserVO;
import com.zr.health.service.UserFollowService;
import com.zr.health.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户关注模块
 * 提供用户关注/取消关注等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/userFollow")
public class UserFollowController {

    @Resource
    private UserFollowService userFollowService;

    @Resource
    private UserService userService;

    /**
     * 关注/取消关注用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 新的关注状态（true=已关注，false=未关注）
     */
    @PostMapping("/toggleFollow")
    public BaseResponse<Boolean> toggleFollow(
            @RequestParam Long followerId,
            @RequestParam Long followingId) {
        boolean newFollowState = userFollowService.toggleFollow(followerId, followingId);
        return ResultUtils.success(newFollowState);
    }

    /**
     * 检查是否已关注
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否已关注
     */
    @GetMapping("/isFollowing")
    public BaseResponse<Boolean> isFollowing(
            @RequestParam Long followerId,
            @RequestParam Long followingId) {
        boolean isFollowing = userFollowService.isFollowing(followerId, followingId);
        return ResultUtils.success(isFollowing);
    }

    /**
     * 获取用户的关注列表（关注的博主）
     * @param followerId 关注者ID
     * @return 关注的博主信息列表
     */
    @GetMapping("/getFollowingList")
    public BaseResponse<List<UserVO>> getFollowingList(@RequestParam Long followerId) {
        List<Long> followingIds = userFollowService.getFollowingList(followerId);
        if (followingIds.isEmpty()) {
            return ResultUtils.success(List.of());
        }
        // 获取博主详细信息
        List<UserVO> followingUsers = followingIds.stream()
                .map(userId -> {
                    try {
                        User user = userService.getById(userId);
                        if (user == null) {
                            return null;
                        }
                        UserVO vo = new UserVO();
                        BeanUtils.copyProperties(user, vo);
                        return vo;
                    } catch (Exception e) {
                        log.error("获取用户信息失败: userId={}", userId, e);
                        return null;
                    }
                })
                .filter(user -> user != null)
                .collect(Collectors.toList());
        return ResultUtils.success(followingUsers);
    }

    /**
     * 获取用户的粉丝列表
     * @param followingId 被关注者ID
     * @return 粉丝信息列表
     */
    @GetMapping("/getFollowerList")
    public BaseResponse<List<UserVO>> getFollowerList(@RequestParam Long followingId) {
        List<Long> followerIds = userFollowService.getFollowerList(followingId);
        if (followerIds.isEmpty()) {
            return ResultUtils.success(List.of());
        }
        // 获取粉丝详细信息
        List<UserVO> followerUsers = followerIds.stream()
                .map(userId -> {
                    try {
                        User user = userService.getById(userId);
                        if (user == null) {
                            return null;
                        }
                        UserVO vo = new UserVO();
                        BeanUtils.copyProperties(user, vo);
                        return vo;
                    } catch (Exception e) {
                        log.error("获取用户信息失败: userId={}", userId, e);
                        return null;
                    }
                })
                .filter(user -> user != null)
                .collect(Collectors.toList());
        return ResultUtils.success(followerUsers);
    }
}

