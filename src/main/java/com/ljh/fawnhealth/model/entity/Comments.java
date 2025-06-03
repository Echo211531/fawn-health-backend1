package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论表（支持多级嵌套与审核）
 * @TableName comments
 */
@TableName(value ="comments")
@Data
public class Comments implements Serializable {
    /**
     * 评论ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 父评论ID，0 表示一级评论
     */
    private Long parentId;

    /**
     * 根评论ID，指向一级评论，便于树结构检索
     */
    private Long rootId;

    /**
     * 被回复的用户ID，用于展示 @xxx
     */
    private Long replyToUserId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 是否作者回复:0否,1是
     */
    private Integer isAuthor;

    /**
     * 评论状态：0正常，1待审，2屏蔽/违规
     */
    private Integer status;

    /**
     * 是否删除:0否,1是
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}