package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 社区帖子表
 * @TableName community_posts
 */
@TableName(value ="community_posts")
@Data
public class CommunityPosts implements Serializable {
    /**
     * 帖子ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面
     */
    private String coverImg;

    /**
     * 是否公开
     */
    private int isPublic;

    /**
     * 内容
     */
    private String content;

    /**
     * 类型:1打卡,2分享,3求助,4成绩单
     */
    private Integer postType;

    /**
     * 图片URL,多个用逗号分隔
     */
    private String images;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 分享数
     */
    private Integer shareCount;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 是否置顶:0否,1是
     */
    private Integer isTop;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除:0否,1是
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}