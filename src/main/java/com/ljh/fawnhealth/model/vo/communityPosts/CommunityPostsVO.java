package com.ljh.fawnhealth.model.vo.communityPosts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ljh.fawnhealth.model.enums.communityPosts.CommunityPostsType;
import lombok.Data;

import java.util.Date;

@Data
public class CommunityPostsVO {

    /**
     * 帖子ID
     */
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
     * 内容
     */
    private String content;

    /**
     * 类型枚举值
     */
    private Integer postType;

    /**
     * 类型描述
     */
    private String postTypeDesc;

    /**
     * 是否公开: 0-私密, 1-公开
     */
    private Boolean isPublic;

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
     * 是否置顶: 0-否, 1-是
     */
    private Boolean isTop;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 帖子类型枚举
     */
    @JsonIgnore
    public CommunityPostsType getPostTypeEnum() {
        return CommunityPostsType.of(postType);
    }
}