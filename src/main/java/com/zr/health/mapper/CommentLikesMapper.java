package com.zr.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zr.health.model.entity.CommentLikes;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author 27105
* @description 针对表【comment_likes(评论点赞表)】的数据库操作Mapper
* @createDate 2025-06-01 22:14:10
* @Entity com.ljh.domain.CommentLikes
*/
public interface CommentLikesMapper extends BaseMapper<CommentLikes> {

    /**
     * 点赞：插入或恢复为 is_delete = 0（upsert）
     */
    @Insert("""
        INSERT INTO comment_likes (comment_id, user_id, is_delete)
        VALUES (#{commentId}, #{userId}, 0)
        ON DUPLICATE KEY UPDATE is_delete = 0, update_time = NOW()
    """)
    void upsertLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 取消点赞：逻辑删除（is_delete = 1）
     */
    @Update("""
        UPDATE comment_likes
        SET is_delete = 1, update_time = NOW()
        WHERE comment_id = #{commentId} AND user_id = #{userId}
    """)
    void markDeleted(@Param("commentId") Long commentId, @Param("userId") Long userId);
}




