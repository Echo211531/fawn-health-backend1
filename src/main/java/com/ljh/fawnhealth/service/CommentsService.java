package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.comments.CommentAddDTO;
import com.ljh.fawnhealth.model.dto.comments.CommentQueryDTO;
import com.ljh.fawnhealth.model.dto.comments.CommentVO;
import com.ljh.fawnhealth.model.entity.Comments;

import java.util.List;

/**
* @author 27105
* @description 针对表【comments(评论表（支持多级嵌套与审核）)】的数据库操作Service
* @createDate 2025-06-01 22:12:13
*/
public interface CommentsService extends IService<Comments> {

    /**
     * 添加评论或回复（支持帖子一级评论和评论的评论）
     * @param dto
     */
    void addComment(CommentAddDTO dto);

    /**
     * 分页查询评论列表，包含楼中楼结构
     *
     * @param dto 查询参数（postId、分页页码、每页大小）
     * @return 评论列表（含子评论）
     */
    List<CommentVO> listCommentsByPostId(CommentQueryDTO dto);

    /**
     * 删除评论（逻辑删除）
     *
     * @param commentId 评论ID
     * @param userId 当前登录用户ID
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 点赞/取消点赞评论（切换状态）
     *
     * @param commentId 评论ID
     * @param userId 当前用户ID
     */
    boolean toggleLike(Long commentId, Long userId);
}
