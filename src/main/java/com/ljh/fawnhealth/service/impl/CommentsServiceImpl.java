package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.handler.PromotionMqHandler;
import com.ljh.fawnhealth.manager.BannedWordsManager;
import com.ljh.fawnhealth.manager.PostBloomFilterManager;
import com.ljh.fawnhealth.mapper.CommentLikesMapper;
import com.ljh.fawnhealth.mapper.CommentsMapper;
import com.ljh.fawnhealth.mapper.CommunityPostsMapper;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.model.dto.comments.CommentAddDTO;
import com.ljh.fawnhealth.model.dto.comments.CommentQueryDTO;
import com.ljh.fawnhealth.model.dto.comments.CommentVO;
import com.ljh.fawnhealth.model.dto.comments.LikeEventDTO;
import com.ljh.fawnhealth.model.entity.CommentLikes;
import com.ljh.fawnhealth.model.entity.Comments;
import com.ljh.fawnhealth.model.entity.CommunityPosts;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.mq.MessageProducer;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.service.CommentsService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现类（支持多级嵌套与逻辑删除、点赞等）
 */
@Service
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments>
        implements CommentsService {

    @Resource
    private CommentsMapper commentsMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CommentLikesMapper commentLikesMapper;

    @Resource
    private CommunityPostsMapper communityPostsMapper;

    @Resource
    private PostBloomFilterManager postBloomFilterManager;

    @Resource
    private BannedWordsManager bannedWordsManager;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MessageProducer messageProducer;


    /**
     * 添加评论或回复（支持帖子一级评论和评论的评论）
     *
     * @param dto 评论数据传输对象，包含帖子ID、评论内容、父评论ID（可为空）、用户ID
     */
    @Override
    public void addComment(CommentAddDTO dto) {
        // 1. 布隆过滤器校验帖子是否存在
        if (!postBloomFilterManager.mightContain(dto.getPostId())) {
            throw new BusinessException(ErrorCode.COMMENT_POST_NOT_FOUND);
        }

        CommunityPosts post = communityPostsMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new BusinessException(ErrorCode.COMMENT_POST_NOT_FOUND);
        }

        // 2. 构建评论实体
        Comments comment = new Comments();
        comment.setPostId(dto.getPostId());
        comment.setUserId(dto.getUserId());
        comment.setContent(dto.getContent());
        comment.setIsDelete(0);
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());

        // 3. 区分一级评论和二级评论
        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;
        comment.setParentId(parentId);

        if (parentId == 0L) {
            // 一级评论，不需要 replyToUserId
            comment.setReplyToUserId(null);
        } else {
            // 二级评论：校验父评论是否存在且属于同一帖子
            Comments parentComment = commentsMapper.selectById(parentId);
            if (parentComment == null || !parentComment.getPostId().equals(dto.getPostId())) {
                throw new BusinessException(ErrorCode.COMMENT_PARENT_INVALID);
            }

            // 二级评论必须设置被回复的人
            if (dto.getReplyToUserId() == null) {
                throw new BusinessException(ErrorCode.COMMENT_REPLY_USER_REQUIRED);
            }
            comment.setReplyToUserId(dto.getReplyToUserId());
        }

        // 4. 设置是否为作者本人
        comment.setIsAuthor(post.getUserId().equals(dto.getUserId()) ? 1 : 0);

        // 5. 检测违禁词并设置状态
        int status = checkContentForViolation(dto.getContent());
        comment.setStatus(status);

        // 6. 插入评论
        commentsMapper.insert(comment);

        // 原子更新帖子评论数（+1）
        communityPostsMapper.update(
                null,
                new UpdateWrapper<CommunityPosts>()
                        .setSql("comment_count = comment_count + 1")
                        .eq("id", dto.getPostId())
        );

        // 7. 清除缓存
        clearCommentCacheByPostId(dto.getPostId());
    }

    /**
     * 违禁词检测
     *
     * @param content
     * @return
     */
    private int checkContentForViolation(String content) {
        if (bannedWordsManager.containsBannedWord(content)) {
            return 2; // 明确违规
        }
        // 这里可以扩展加入“可疑词”判断逻辑
        return 0; // 正常
    }

    /**
     * 分页查询评论列表，包含楼中楼结构
     *
     * @param dto 查询参数（postId、分页页码、每页大小）
     * @return
     */
    @Override
    public List<CommentVO> listCommentsByPostId(CommentQueryDTO dto) {
        // 构建Redis缓存键，格式为"comments:post:{postId}:page:{pageNum}"
        String redisKey = "comments:post:" + dto.getPostId() + ":page:" + dto.getPageNum();
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        // 1. 优先从Redis缓存中获取评论列表
        Object cache = ops.get(redisKey);
        if (cache != null) {
            // 缓存命中，直接返回缓存数据
            return (List<CommentVO>) cache;
        }

        // 2. 缓存未命中，查询数据库
        // 创建分页对象，指定页码和每页大小
        Page<Comments> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        // 构建查询条件：查询指定帖子下的一级评论（parent_id=0），且状态正常、未删除，按创建时间倒序排列
        QueryWrapper<Comments> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", dto.getPostId())
                .eq("parent_id", 0)
                .eq("status", 0)
                .eq("is_delete", 0)
                .orderByDesc("create_time");

        // 执行分页查询获取一级评论列表
        List<Comments> topComments = commentsMapper.selectPage(page, wrapper).getRecords();
        if (topComments.isEmpty()) {
            // 若无一级评论，直接返回空列表
            return Collections.emptyList();
        }

        // 获取所有一级评论的ID列表，用于查询子评论
        List<Long> topIds = topComments.stream().map(Comments::getId).toList();

        // 查询所有一级评论的子评论，条件为父ID在一级评论ID列表中，状态正常、未删除，按创建时间正序排列
        List<Comments> childComments = commentsMapper.selectList(
                new QueryWrapper<Comments>()
                        .in("parent_id", topIds)
                        .eq("status", 0)
                        .eq("is_delete", 0)
                        .orderByAsc("create_time")
        );

        // 收集所有评论涉及的用户ID（去重），用于批量查询用户信息
        Set<Long> userIds = new HashSet<>();
        topComments.forEach(c -> userIds.add(c.getUserId()));
        childComments.forEach(c -> userIds.add(c.getUserId()));

        // 批量查询用户信息并构建ID到用户对象的映射表
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 将子评论按父ID分组，构建父ID到子评论VO列表的映射
        Map<Long, List<CommentVO>> childMap = new HashMap<>();
        for (Comments child : childComments) {
            // 转换子评论为VO对象（包含用户信息和当前用户是否已点赞）
            CommentVO vo = toVO(child, userMap, dto.getCurrentUserId());
            // 将VO对象添加到对应的父评论分组中
            childMap.computeIfAbsent(child.getParentId(), k -> new ArrayList<>()).add(vo);
        }

        // 将一级评论转换为VO对象，并关联其子评论列表
        List<CommentVO> result = topComments.stream().map(top -> {
            CommentVO vo = toVO(top, userMap, dto.getCurrentUserId());
            vo.setReplies(childMap.getOrDefault(top.getId(), new ArrayList<>()));
            return vo;
        }).collect(Collectors.toList());

        // 3. 将组装好的评论列表存入Redis缓存，设置5分钟过期时间
        ops.set(redisKey, result, 5, java.util.concurrent.TimeUnit.MINUTES);
        // 返回结果
        return result;
    }


    /**
     * 删除评论（逻辑删除）
     *
     * @param commentId 评论ID
     * @param userId 当前登录用户ID
     */
    @Override
    public void deleteComment(Long commentId, Long userId) {
        // 查询评论是否存在
        Comments comment = commentsMapper.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 查询帖子
        CommunityPosts post = communityPostsMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 判断权限：
        // 1. 是帖子作者
        // 2. 是评论作者
        boolean isPostOwner = post.getUserId().equals(userId);
        boolean isCommentOwner = comment.getUserId().equals(userId);
        if (!isPostOwner && !isCommentOwner) {
            throw new BusinessException(ErrorCode.NO_COMMENTS_AUTH_ERROR);
        }

        // 执行逻辑删除
        comment.setIsDelete(1);
        comment.setUpdateTime(new Date());
        commentsMapper.updateById(comment);

        // 原子更新帖子评论数（-1）
        communityPostsMapper.update(
                null,
                new UpdateWrapper<CommunityPosts>()
                        .setSql("comment_count = comment_count - 1")
                        .eq("id", comment.getPostId())
                        .gt("comment_count", 0) // 防止评论数为负数
        );

        // 清除缓存
        clearCommentCacheByPostId(comment.getPostId());
    }



    /**
     * 点赞/取消点赞评论（幂等处理）
     *
     * @param commentId 评论ID
     * @param userId 当前用户ID
     * @return
     */
    @Override
    public boolean toggleLike(Long commentId, Long userId) {
        String likeCountKey = "comment:like:count:" + commentId;
        String userLikedKey = "comment:liked:user:" + userId;

        Boolean isMember = redisTemplate.opsForSet().isMember(userLikedKey, commentId);
        boolean liked = Boolean.TRUE.equals(isMember);

        Comments comment = commentsMapper.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        boolean newLikeState;
        if (liked) {
            // 取消点赞
            redisTemplate.opsForSet().remove(userLikedKey, commentId);
            redisTemplate.opsForValue().decrement(likeCountKey);
            newLikeState = false;
        } else {
            // 点赞
            redisTemplate.opsForSet().add(userLikedKey, commentId);
            redisTemplate.opsForValue().increment(likeCountKey);
            newLikeState = true;
        }

        // 发送消息到 MQ，异步写库
        LikeEventDTO likeEvent = new LikeEventDTO(commentId, userId, newLikeState);
        messageProducer.sendLikeMessage(
                MqConstant.COMMENT_LIKE_ROUTING_KEY,
                likeEvent
        );

        clearCommentCacheByPostId(comment.getPostId());

        return newLikeState;
    }




    /**
     * 将评论实体转换为视图对象 VO
     *
     * @param comment
     * @param userMap
     * @return
     */
    private CommentVO toVO(Comments comment, Map<Long, User> userMap, Long currentUserId) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        User user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        // 使用当前登录用户ID判断点赞状态
        if (currentUserId != null) {
            String userLikedKey = "comment:liked:user:" + currentUserId;
            Boolean liked = redisTemplate.opsForSet().isMember(userLikedKey, comment.getId());
            vo.setLiked(Boolean.TRUE.equals(liked));
        } else {
            vo.setLiked(false);
        }

        String likeCountKey = "comment:like:count:" + comment.getId();
        Object countObj = redisTemplate.opsForValue().get(likeCountKey);
        if (countObj instanceof Integer count) {
            vo.setLikeCount(count);
        } else if (countObj instanceof Long longCount) {
            vo.setLikeCount(longCount.intValue());
        } else {
            vo.setLikeCount(comment.getLikeCount());
        }

        return vo;
    }



    private void clearCommentCacheByPostId(Long postId) {
        String pattern = "comments:post:" + postId + ":page:*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

}
