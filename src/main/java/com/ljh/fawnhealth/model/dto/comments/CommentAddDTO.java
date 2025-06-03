package com.ljh.fawnhealth.model.dto.comments;

import lombok.Data;

/**
 * 评论添加请求参数
 */
@Data
public class CommentAddDTO {

    /**
     * 评论者ID
     */
    private Long userId;

    /**
     * 被评论的帖子ID
     */
    private Long postId;

    /**
     * 父评论ID（0 表示一级评论）
      */
    private Long parentId = 0L;

    /**
     * 被回复的用户ID（用于显示“回复某人”）
     */
    private Long replyToUserId;

    /**
     * 评论内容
     */
    private String content;
}
