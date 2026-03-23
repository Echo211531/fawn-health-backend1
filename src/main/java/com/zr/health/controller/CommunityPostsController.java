package com.zr.health.controller;

import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.exception.ErrorCode;
import com.zr.health.exception.ThrowUtils;
import com.zr.health.model.entity.CommunityPosts;
import com.zr.health.model.vo.communityPosts.CommunityPostsVO;
import com.zr.health.service.CommunityPostsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

/**
 * 社区帖子模块
 * 提供社区帖子的增删改查、点赞等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/communityPosts")
public class CommunityPostsController {

    @Resource
    private CommunityPostsService communityPostsService;

    /**
     * 查询全部公开帖子信息
     *
     * @return 统一响应对象，包含帖子视图对象列表
     */
    @GetMapping("/selectAllCommunityPosts")
    public BaseResponse<List<CommunityPostsVO>> selectAllCommunityPosts() {
        List<CommunityPostsVO> list = communityPostsService.selectAllCommunityPosts();
        return ResultUtils.success(list);
    }

    /**
     * 根据帖子ID查询帖子详情(管理员使用)
     *
     * @param postId 帖子ID，不能为空
     * @return 统一响应对象，包含帖子详情视图对象
     */
    @GetMapping("/getCommunityPostsByIdAdmin")
    public BaseResponse<CommunityPostsVO> getCommunityPostsByIdAdmin(@RequestParam Long postId) {
        CommunityPostsVO postVO = communityPostsService.getCommunityPostsByIdAdmin(postId);
        return ResultUtils.success(postVO);
    }

    /**
     * 根据帖子ID查询帖子详情(用户使用)
     *
     * @param postId 帖子ID，不能为空
     * @return 统一响应对象，包含帖子详情视图对象
     */
    @GetMapping("/getCommunityPostsById")
    public BaseResponse<CommunityPostsVO> getCommunityPostsById(@RequestParam Long postId, @RequestParam int isPublic) {
        CommunityPostsVO postVO = communityPostsService.getCommunityPostsById(postId,isPublic);
        return ResultUtils.success(postVO);
    }

    /**
     * 帖子浏览量 +1（进入详情页时调用）
     *
     * @param postId 帖子ID
     * @return 更新后的浏览量
     */
    @PostMapping("/incrementView")
    public BaseResponse<Integer> incrementView(@RequestParam Long postId) {
        ThrowUtils.throwIf(postId == null, ErrorCode.PARAMS_ERROR, "帖子ID不能为空");
        Integer count = communityPostsService.incrementPostView(postId);
        return ResultUtils.success(count);
    }

//    /**
//     * 帖子点赞接口
//     *
//     * @param postId 被点赞的帖子ID
//     * @param userId 执行点赞操作的用户ID
//     * @return 统一响应对象，包含操作结果（成功/失败）
//     */
//    @PostMapping("/likeCommunityPosts")
//    public BaseResponse<Boolean> likeCommunityPosts(@RequestParam Long postId, Long userId) {
//        // 校验用户ID是否存在
//        ThrowUtils.throwIf(userId == null, ErrorCode.USER_NOTFOUND);
//        boolean result = communityPostsService.likeCommunityPosts(postId, userId);
//        return ResultUtils.success(result);
//    }

//    /**
//     * 取消帖子点赞接口
//     *
//     * @param postId 被取消点赞的帖子ID
//     * @param userId 执行取消点赞操作的用户ID
//     * @return 统一响应对象，包含操作结果（成功/失败）
//     */
//    @PostMapping("/unlikeCommunityPosts")
//    public BaseResponse<Boolean> unlikeCommunityPosts(@RequestParam Long postId, Long userId) {
//        // 校验用户ID是否存在
//        ThrowUtils.throwIf(userId == null, ErrorCode.USER_NOTFOUND);
//        boolean result = communityPostsService.unlikeCommunityPosts(postId, userId);
//        return ResultUtils.success(result);
//    }

    /**
     * 点赞/取消点赞帖子（无需额外参数，自动判断状态）
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 操作结果（true=点赞，false=取消点赞）
     */
    @PostMapping("/toggleLike")
    public BaseResponse<Boolean> toggleLike(
            @RequestParam Long postId,
            @RequestParam Long userId
    ) {
        // 校验用户ID
        ThrowUtils.throwIf(userId == null, ErrorCode.USER_NOTFOUND);

        // 调用服务层方法，自动判断当前状态并切换
        boolean newLikeState = communityPostsService.toggleLike(postId, userId);

        return ResultUtils.success(newLikeState);
    }

    /**
     * 检查用户是否点赞过某帖子
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 是否已点赞（true=已点赞，false=未点赞）
     */
    @GetMapping("/hashCommunityPosts")
    public BaseResponse<Boolean> hashCommunityPosts(
            @RequestParam Long postId,
            @RequestParam Long userId
    ) {
        // 校验参数
        ThrowUtils.throwIf(postId == null, ErrorCode.PARAMS_ERROR, "帖子ID不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.USER_NOTFOUND);

        // 调用服务层方法检查点赞状态
        Boolean hasLiked = communityPostsService.hashCommunityPosts(postId, userId);

        return ResultUtils.success(hasLiked != null && hasLiked);
    }

    /**
     * 发布新帖子接口
     *
     * @param communityPosts 帖子实体对象，包含帖子内容、标题等信息
     * @return 统一响应对象，包含新帖子的ID
     */
    @PostMapping("/addCommunityPosts")
    public BaseResponse<Long> addCommunityPosts(@RequestBody CommunityPosts communityPosts) {
        // 设置帖子的发布用户
        communityPosts.setUserId(communityPosts.getUserId());
        Long postId = communityPostsService.publishPost(communityPosts);
        return ResultUtils.success(postId);
    }


//    /**
//     * 修改帖子接口
//     *
//     * @param communityPosts 更新后的帖子实体对象，必须包含帖子ID
//     * @return 修改后的帖子视图对象
//     */
//    @PostMapping("/updatePost")
//    public BaseResponse<CommunityPostsVO> updatePost(@RequestBody CommunityPosts communityPosts) {
//        // 参数校验
//        ThrowUtils.throwIf(communityPosts.getId() == null, ErrorCode.COMMUNITY_POST_NOT_FOUND);
//        // 执行更新并返回更新后的视图对象
//        CommunityPostsVO updatedPostVO = communityPostsService.updatePostAndReturnVO(communityPosts);
//        return ResultUtils.success(updatedPostVO);
//    }

    /**
     * 修改帖子接口
     *
     * @param communityPosts 更新后的帖子实体对象，必须包含帖子ID
     * @return 修改后的帖子视图对象
     */
    @PostMapping("/updateCommunityPosts")
    public BaseResponse<CommunityPostsVO> updateCommunityPosts(@RequestBody CommunityPosts communityPosts, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(communityPosts.getId() == null, ErrorCode.COMMUNITY_POST_NOT_FOUND);

        // 校验用户权限
        CommunityPostsVO existingPost = communityPostsService.getCommunityPostsById(communityPosts.getId(),communityPosts.getIsPublic());
        ThrowUtils.throwIf(existingPost == null, ErrorCode.COMMUNITY_POST_NOT_FOUND);
        ThrowUtils.throwIf(!existingPost.getUserId().equals(userId), ErrorCode.PARAMS_ERROR, "无权修改他人帖子");

        // 设置更新人ID
        communityPosts.setUserId(userId);

        // 执行更新
        CommunityPostsVO updatedPostVO = communityPostsService.updatePostAndReturnVO(communityPosts);
        return ResultUtils.success(updatedPostVO);
    }


    /**
     * 获取热点帖子排行榜
     *
     * @param topN 需要获取的热点帖子数量
     * @return 统一响应对象，包含按热度排序的帖子视图对象列表
     */
    @GetMapping("/hotRanking")
    public BaseResponse<List<CommunityPostsVO>> getHotPostRanking(@RequestParam(required = false, defaultValue = "10") int topN) {
        // 参数校验
        ThrowUtils.throwIf(topN <= 0, ErrorCode.PARAMS_ERROR, "topN参数必须大于0");
        // 调用业务层获取热点帖子
        List<CommunityPostsVO> hotPosts = communityPostsService.getHotPosts(topN);
        return ResultUtils.success(hotPosts);
    }

    /**
     * 根据用户ID查询帖子列表
     *
     * @param userId 用户ID
     * @param isPublic 是否只查询公开帖子(1:公开,0:私密)
     * @return 统一响应对象，包含帖子视图对象列表
     */
    @GetMapping("/listByUserId")
    public BaseResponse<List<CommunityPostsVO>> listPostsByUserId(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer isPublic) {  // 移除默认值，改为可选参数
        List<CommunityPostsVO> list = communityPostsService.listPostsByUserId(userId, isPublic);
        return ResultUtils.success(list);
    }

    /**
     * 删除帖子接口
     * @param postId 要删除的帖子ID
     * @param userId 当前操作用户ID
     * @return 统一响应对象，包含操作结果
     */
    @PostMapping("/deletePost")
    public BaseResponse<Boolean> deletePost(@RequestParam Long postId, @RequestParam Long userId) {
        // 校验用户ID
        ThrowUtils.throwIf(userId == null, ErrorCode.USER_NOTFOUND);

        boolean result = communityPostsService.deletePost(postId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 根据关键词搜索帖子
     * @param keyword 搜索关键词
     * @return 匹配的帖子列表
     */
    @GetMapping("/search")
    public BaseResponse<List<CommunityPostsVO>> searchPosts(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<CommunityPostsVO> posts = communityPostsService.searchPosts(keyword.trim());
        return ResultUtils.success(posts);
    }

    /**
     * 根据关注的用户ID列表获取帖子
     * @param followingIds 关注的用户ID列表（逗号分隔）
     * @return 帖子列表
     */
    @GetMapping("/getPostsByFollowingIds")
    public BaseResponse<List<CommunityPostsVO>> getPostsByFollowingIds(@RequestParam String followingIds) {
        List<Long> ids = List.of();
        if (followingIds != null && !followingIds.trim().isEmpty()) {
            ids = Arrays.stream(followingIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
        }
        List<CommunityPostsVO> posts = communityPostsService.getPostsByFollowingIds(ids);
        return ResultUtils.success(posts);
    }

}