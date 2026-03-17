package com.zr.health.model.dto.comments;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 评论返回 VO（视图对象）
 */
@Data
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论用户昵称
     */
    private String nickname;

    /**
     * 评论用户头像
     */
    private String avatar;

    /**
     * 是否作者回复（0否，1是）
     */
    private Integer isAuthor;

    /**
     * 父评论ID（为 null 表示一级评论）
     */
    private Long parentId;

    /**
     * 点赞数量
     */
    private Integer likeCount;

    /**
     * 当前登录用户是否已点赞
     */
    private Boolean liked;

    /**
     * 被评论人的ID
     */
    private Long replyToUserId;

    /**
     * 被评论人的昵称
     */
    private String replyToUserNickname;

    /**
     * 评论时间
     */
    private Date createTime;

    /**
     * 子评论列表
     */
    private List<CommentVO> replies;
}
