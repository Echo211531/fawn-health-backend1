package com.zr.health.model.dto.comments;

import lombok.Data;

import java.io.Serializable;

/**
 * 评论查询参数 DTO
 */
@Data
public class CommentQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 当前页码（默认第1页）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10条）
     */
    private Integer pageSize = 10;

    /**
     * 当前登录用户ID（用于判断点赞状态）
     */
    private Long currentUserId;
}
