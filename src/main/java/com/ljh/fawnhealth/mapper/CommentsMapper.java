package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.Comments;
import org.apache.ibatis.annotations.Param;

/**
* @author 27105
* @description 针对表【comments(评论表（支持多级嵌套与审核）)】的数据库操作Mapper
* @createDate 2025-06-01 22:12:13
* @Entity com.ljh.domain.Comments
*/
public interface CommentsMapper extends BaseMapper<Comments> {

    /**
     * 点赞数 +1
     * @param commentId 评论ID
     * @return 影响行数
     */
    int increaseLikeCount(@Param("commentId") Long commentId);

    /**
     * 点赞数 -1（不能低于 0）
     * @param commentId 评论ID
     * @return 影响行数
     */
    int decreaseLikeCount(@Param("commentId") Long commentId);

}




