package com.zr.health.controller;

import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.model.dto.comments.CommentAddDTO;
import com.zr.health.model.dto.comments.CommentQueryDTO;
import com.zr.health.model.dto.comments.CommentVO;
import com.zr.health.service.CommentsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论模块控制器
 * 提供评论新增、查询、删除、点赞等功能
 */
@Slf4j
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentsService commentsService;

    /**
     * 添加评论或回复（支持帖子一级评论和评论的评论）
     *
     * @param dto
     * @param
     */
    @PostMapping("/addComment")
    public BaseResponse<String> addComment(@RequestBody CommentAddDTO dto) {
        commentsService.addComment(dto);
        return ResultUtils.success("评论成功");
    }

    /**
     * 分页查询评论列表，包含楼中楼结构
     *
     * @param dto 查询参数（postId、分页页码、每页大小）
     * @return 评论列表（含子评论）
     */
    @PostMapping("/listComments")
    public BaseResponse<List<CommentVO>> listComments(@RequestBody CommentQueryDTO dto) {
        List<CommentVO> comments = commentsService.listCommentsByPostId(dto);
        return ResultUtils.success(comments);
    }

    /**
     * 删除评论（逻辑删除）
     *
     * @param commentId 评论ID
     * @param userId 当前登录用户ID
     */
    @PostMapping("/deleteComment")
    public BaseResponse<String> deleteComment(Long commentId, Long userId) {
        commentsService.deleteComment(commentId, userId);
        return ResultUtils.success("删除成功");
    }

    /**
     * 点赞/取消点赞评论（切换状态）
     *
     * @param commentId 评论ID
     * @param userId 当前用户ID
     */
    @PostMapping("/likeComment")
    public BaseResponse<String> likeComment(
            @RequestParam("commentId") Long commentId,
            @RequestParam("userId") Long userId) {
        boolean liked = commentsService.toggleLike(commentId, userId);
        return liked ? ResultUtils.success("点赞成功") : ResultUtils.success("取消点赞成功");
    }

    /**
     * 根据用户ID查询用户的所有评论（App端使用，不分页）
     *
     * @param userId 用户ID
     */
    @GetMapping("/listByUser")
    public BaseResponse<List<CommentVO>> listCommentsByUser(@RequestParam Long userId) {
        List<CommentVO> comments = commentsService.listCommentsByUserId(userId);
        return ResultUtils.success(comments);
    }
}
