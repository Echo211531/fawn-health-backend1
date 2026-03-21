package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zr.health.context.BaseContext;
import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.manager.BannedWordsManager;
import com.zr.health.manager.PostBloomFilterManager;
import com.zr.health.mapper.CommentsMapper;
import com.zr.health.mapper.CommunityPostsMapper;
import com.zr.health.mapper.UserMapper;
import com.zr.health.model.dto.comments.CommentAddDTO;
import com.zr.health.model.dto.comments.CommentQueryDTO;
import com.zr.health.model.dto.comments.CommentVO;
import com.zr.health.model.dto.comments.LikeEventDTO;
import com.zr.health.model.entity.Comments;
import com.zr.health.model.entity.CommunityPosts;
import com.zr.health.model.entity.User;
import com.zr.health.mq.MessageProducer;
import com.zr.health.mq.MqConstant;
import com.zr.health.service.CommentsService;
import com.zr.health.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;

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
    private CommunityPostsMapper communityPostsMapper;

    @Resource(name = "objectRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private PostBloomFilterManager postBloomFilterManager;

    @Resource
    private BannedWordsManager bannedWordsManager;

    @Resource
    private UserService userService;

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
        Long userId = BaseContext.getCurrentId();
        //comment.setUserId(dto.getUserId());
        comment.setUserId(userId);
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
     * @return 评论VO列表
     */
    @Override
    public List<CommentVO> listCommentsByPostId(CommentQueryDTO dto) {
        String sortKey = normalizeCommentSortBy(dto.getSortBy());
        String redisKey = "comments:post:" + dto.getPostId() + ":page:" + dto.getPageNum() + ":sort:" + sortKey;
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        Object cache = ops.get(redisKey);
        if (cache != null) {
            return (List<CommentVO>) cache;
        }

        // 1. 查询所有评论
        List<Comments> allComments = commentsMapper.selectList(
                new QueryWrapper<Comments>()
                        .eq("post_id", dto.getPostId())
                        .eq("status", 0)
                        .eq("is_delete", 0)
                        .orderByAsc("create_time")
        );
        if (allComments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按 parentId 分组
        Map<Long, List<Comments>> parentIdMap = allComments.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));

        // 3. 用户信息
        Set<Long> userIds = allComments.stream().map(Comments::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. 所有评论Map
        Map<Long, Comments> allCommentsMap = allComments.stream()
                .collect(Collectors.toMap(Comments::getId, c -> c));

        // 5. 一级评论排序后分页
        List<Comments> topComments = new ArrayList<>(parentIdMap.getOrDefault(0L, new ArrayList<>()));
        if ("like".equals(sortKey)) {
            topComments.sort(comparing((Comments c) -> c.getLikeCount() != null ? c.getLikeCount() : 0, reverseOrder())
                    .thenComparing(Comments::getCreateTime, nullsLast(reverseOrder())));
        } else {
            topComments.sort(comparing(Comments::getCreateTime, nullsLast(reverseOrder())));
        }
        int fromIndex = (dto.getPageNum() - 1) * dto.getPageSize();
        int toIndex = Math.min(fromIndex + dto.getPageSize(), topComments.size());
        if (fromIndex >= topComments.size()) {
            return Collections.emptyList();
        }
        List<Comments> pageTopComments = topComments.subList(fromIndex, toIndex);

        // 6. 平铺组装
        List<CommentVO> result = new ArrayList<>();
        for (Comments top : pageTopComments) {
            CommentVO vo = convertToVO(top, userMap, dto.getCurrentUserId(), allCommentsMap);
            vo.setReplies(collectAllRepliesFlat(top.getId(), parentIdMap, userMap, dto.getCurrentUserId(), allCommentsMap));
            result.add(vo);
        }

//        // 6. 递归组装
//        List<CommentVO> result = new ArrayList<>();
//        for (Comments top : pageTopComments) {
//            CommentVO vo = convertToVO(top, userMap, dto.getCurrentUserId(), allCommentsMap);
//            vo.setReplies(buildTree(top.getId(), parentIdMap, userMap, dto.getCurrentUserId(), allCommentsMap));
//            result.add(vo);
//        }

        ops.set(redisKey, result, 5, TimeUnit.MINUTES);
        return result;
    }

    // 递归方法
    private List<CommentVO> buildTree(Long parentId, Map<Long, List<Comments>> parentIdMap,
                                      Map<Long, User> userMap, Long currentUserId, Map<Long, Comments> allCommentsMap) {
        List<Comments> children = parentIdMap.getOrDefault(parentId, new ArrayList<>());
        List<CommentVO> result = new ArrayList<>();
        for (Comments c : children) {
            CommentVO vo = convertToVO(c, userMap, currentUserId, allCommentsMap);
            vo.setReplies(buildTree(c.getId(), parentIdMap, userMap, currentUserId, allCommentsMap));
            result.add(vo);
        }
        return result;
    }

    // 新增方法
    private List<CommentVO> collectAllRepliesFlat(Long parentId, Map<Long, List<Comments>> parentIdMap,
                                                  Map<Long, User> userMap, Long currentUserId, Map<Long, Comments> allCommentsMap) {
        List<CommentVO> result = new ArrayList<>();
        Queue<Comments> queue = new LinkedList<>(parentIdMap.getOrDefault(parentId, new ArrayList<>()));
        while (!queue.isEmpty()) {
            Comments c = queue.poll();
            CommentVO vo = convertToVO(c, userMap, currentUserId, allCommentsMap);
            vo.setReplies(null); // 或 Collections.emptyList()
            result.add(vo);
            queue.addAll(parentIdMap.getOrDefault(c.getId(), new ArrayList<>()));
        }
        return result;
    }

    /**
     * 删除评论（逻辑删除）
     *
     * @param commentId 评论ID
     * @param userId    当前登录用户ID
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
     * 根据用户ID查询用户的所有评论（App端使用，不分页）
     *
     * @param userId 用户ID
     * @return
     */
    @Override
    public List<CommentVO> listCommentsByUserId(Long userId) {
        // 1. 查询所有未删除的评论（按时间倒序）
        List<Comments> comments = commentsMapper.selectByUserId(userId);

        // 2. 批量查询关联用户信息（优化性能）
        Set<Long> userIds = comments.stream()
                .flatMap(comment -> Stream.of(
                        comment.getUserId(),
                        comment.getReplyToUserId()
                ))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userService.getUserMapByIds(userIds);

        // 3. 转换为VO
        return comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(comment, vo);

            // 设置用户信息
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setAvatar(user.getAvatar());
                vo.setNickname(user.getNickname());
            }

            // 设置被回复用户信息
            if (comment.getReplyToUserId() != null) {
                User replyUser = userMap.get(comment.getReplyToUserId());
                if (replyUser != null) {
                    vo.setReplyToUserNickname(replyUser.getNickname());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 将评论实体转换为视图对象VO
     *
     * @param comment       评论实体
     * @param userMap       用户ID到用户对象的映射
     * @param currentUserId 当前登录用户ID
     * @return 评论VO对象
     */
    private CommentVO convertToVO(Comments comment, Map<Long, User> userMap, Long currentUserId, Map<Long, Comments> allCommentsMap) {
        CommentVO vo = new CommentVO();
        // 复制基本属性
        BeanUtils.copyProperties(comment, vo);

        // 设置用户信息
        User user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        // 设置当前用户是否点赞
        if (currentUserId != null) {
            String userLikedKey = "comment:liked:user:" + currentUserId;
            Boolean liked = redisTemplate.opsForSet().isMember(userLikedKey, comment.getId());
            vo.setLiked(Boolean.TRUE.equals(liked));
        } else {
            vo.setLiked(false);
        }

        // 设置点赞数（优先从Redis获取）
        String likeCountKey = "comment:like:count:" + comment.getId();
        Object countObj = redisTemplate.opsForValue().get(likeCountKey);
        if (countObj instanceof Integer count) {
            vo.setLikeCount(count);
        } else if (countObj instanceof Long longCount) {
            vo.setLikeCount(longCount.intValue());
        } else {
            vo.setLikeCount(comment.getLikeCount());
        }

        // 设置被回复用户信息（仅对子评论/楼中楼有效）
        if (comment.getParentId() != null && comment.getParentId() != 0) {
            Comments parent = allCommentsMap.get(comment.getParentId());
            if (parent != null) {
                vo.setReplyToUserId(parent.getUserId());
                User replyToUser = userMap.get(parent.getUserId());
                vo.setReplyToUserNickname(replyToUser != null ? replyToUser.getNickname() : null);
            }
        }

        return vo;
    }

    /**
     * 规范化评论排序参数：like / time
     */
    private static String normalizeCommentSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "like";
        }
        String s = sortBy.trim().toLowerCase();
        if ("time".equals(s)) {
            return "time";
        }
        return "like";
    }

    private void clearCommentCacheByPostId(Long postId) {
        // 清除该帖下所有分页与排序维度的评论列表缓存
        String pattern = "comments:post:" + postId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}