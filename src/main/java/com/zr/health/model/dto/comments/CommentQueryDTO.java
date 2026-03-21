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

    /**
     * 一级评论排序：like-按热度（点赞数降序，同赞按时间降序）；time-按时间（最新在前）
     */
    private String sortBy = "like";
}
