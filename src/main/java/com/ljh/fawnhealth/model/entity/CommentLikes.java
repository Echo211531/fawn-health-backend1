package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论点赞表
 * @TableName comment_likes
 */
@TableName(value ="comment_likes")
@Data
public class CommentLikes implements Serializable {
    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 点赞用户ID
     */
    private Long userId;

    /**
     * 点赞时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除:0否,1是（取消点赞）
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}