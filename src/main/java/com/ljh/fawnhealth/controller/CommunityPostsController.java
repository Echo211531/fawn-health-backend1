package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.context.BaseContext;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.exception.ThrowUtils;
import com.ljh.fawnhealth.model.entity.CommunityPosts;
import com.ljh.fawnhealth.model.vo.communityPosts.CommunityPostsVO;
import com.ljh.fawnhealth.service.CommunityPostsService;
import com.ljh.fawnhealth.service.CouponsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区帖子控制器
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
     * 帖子点赞接口
     *
     * @param postId 被点赞的帖子ID
     * @param userId 执行点赞操作的用户ID
     * @return 统一响应对象，包含操作结果（成功/失败）
     */
    @PostMapping("/likeCommunityPosts")
    public BaseResponse<Boolean> likeCommunityPosts(@RequestParam Long postId, Long userId) {
        // 校验用户ID是否存在
        ThrowUtils.throwIf(userId == null, ErrorCode.USER_NOTFOUND);
        boolean result = communityPostsService.likeCommunityPosts(postId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 取消帖子点赞接口
     *
     * @param postId 被取消点赞的帖子ID
     * @param userId 执行取消点赞操作的用户ID
     * @return 统一响应对象，包含操作结果（成功/失败）
     */
    @PostMapping("/unlikeCommunityPosts")
    public BaseResponse<Boolean> unlikeCommunityPosts(@RequestParam Long postId, Long userId) {
        // 校验用户ID是否存在
        ThrowUtils.throwIf(userId == null, ErrorCode.USER_NOTFOUND);
        boolean result = communityPostsService.unlikeCommunityPosts(postId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 发布新帖子接口
     *
     * @param communityPosts 帖子实体对象，包含帖子内容、标题等信息
     * @return 统一响应对象，包含新帖子的ID
     */
    @PostMapping("/publishPost")
    public BaseResponse<Long> publishPost(@RequestBody CommunityPosts communityPosts) {
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
    @PostMapping("/updatePost")
    public BaseResponse<CommunityPostsVO> updatePost(@RequestBody CommunityPosts communityPosts, Long userId) {
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




}