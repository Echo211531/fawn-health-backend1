package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.model.entity.CommunityPosts;
import com.ljh.fawnhealth.model.vo.communityPosts.CommunityPostsVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 社区帖子服务接口
 * 提供社区帖子的增删改查、点赞管理等业务逻辑操作
 */
public interface CommunityPostsService extends IService<CommunityPosts> {

    /**
     * 查询全部公开帖子信息
     *
     * @return 帖子视图对象列表，包含帖子基本信息、作者信息、点赞数等
     */
    List<CommunityPostsVO> selectAllCommunityPosts();

    /**
     * 根据帖子ID查询帖子详情
     *
     * @param postId 帖子ID，不能为空
     * @return 帖子详情视图对象，包含完整的帖子信息和相关统计数据
     */
    CommunityPostsVO getCommunityPostsById(Long postId,Integer isPublic);


//    /**
//     * 帖子点赞功能
//     *
//     * @param postId 被点赞的帖子ID
//     * @param userId 执行点赞操作的用户ID
//     * @return 操作结果，true表示点赞成功，false表示失败（如已点赞）
//     */
//    boolean likeCommunityPosts(Long postId, Long userId);

    /**
     * 获取热点帖子列表
     * 使用HeavyKeeper算法获取TopK热点
     */
    List<CommunityPostsVO> getHotPosts(int topN);

//    /**
//     * 取消帖子点赞
//     *
//     * @param postId 被取消点赞的帖子ID
//     * @param userId 执行取消点赞操作的用户ID
//     * @return 操作结果，true表示取消成功，false表示失败
//     */
//    boolean unlikeCommunityPosts(Long postId, Long userId);

    /**
     * 检查用户是否已点赞某个帖子
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return true表示已点赞，false表示未点赞
     */
    Boolean hashCommunityPosts(Long postId, Long userId);

    /**
     * 批量检查用户是否已点赞多个帖子
     *
     * @param userId  用户ID
     * @param postIds 帖子ID列表
     * @return Map<帖子ID, 是否点赞>，键为帖子ID，值为是否点赞的布尔值
     */
    Map<Long, Boolean> hasLikedPostsBatch(Long userId, List<Long> postIds);

    /**
     * 发布新帖子
     *
     * @param communityPosts 帖子实体对象，包含帖子标题、内容、作者等信息
     * @return 新发布帖子的ID
     */
    Long publishPost(CommunityPosts communityPosts);

    /**
     * 修改帖子并返回最新的帖子视图对象。
     *
     * @param communityPosts 包含修改信息的帖子实体对象，必须包含有效的帖子ID
     * @return 修改后的帖子视图对象（CommunityPostsVO）
     * @throws BusinessException 若帖子不存在或更新失败
     */
    CommunityPostsVO updatePostAndReturnVO(CommunityPosts communityPosts);

    /**
     * 根据帖子ID查询帖子详情（管理员使用）
     *
     * @param postId 帖子ID，不能为空
     * @return 帖子详情视图对象，包含完整的帖子信息和相关统计数据
     */
    CommunityPostsVO getCommunityPostsByIdAdmin(Long postId);


    boolean toggleLike(Long postId, Long userId);
}