package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.constant.CommunityPostsConstant;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.exception.ThrowUtils;
import com.ljh.fawnhealth.manager.cache.CacheManager;
import com.ljh.fawnhealth.mapper.CommunityPostsMapper;
import com.ljh.fawnhealth.mapper.PostLikesMapper;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.model.entity.CommunityPosts;
import com.ljh.fawnhealth.model.entity.PostLikes;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.enums.communityPosts.CommunityPostsType;
import com.ljh.fawnhealth.model.vo.communityPosts.CommunityPostsVO;
import com.ljh.fawnhealth.service.CommunityPostsService;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import com.ljh.fawnhealth.manager.PostBloomFilterManager;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社区帖子服务实现类
 * 包含帖子的查询、点赞、缓存管理等核心功能
 */
@Slf4j
@Service
public class CommunityPostsServiceImpl extends ServiceImpl<CommunityPostsMapper, CommunityPosts>
        implements CommunityPostsService {

    @Resource
    private CommunityPostsMapper communityPostsMapper; // 帖子数据库操作接口

    @Resource
    private PostLikesMapper postLikesMapper; // 点赞记录数据库操作接口

    @Resource
    private RedisTemplate<String, Object> redisTemplate; // Redis缓存模板

    @Resource
    @Qualifier("redisExecutor")
    private Executor redisExecutor; // Redis异步操作线程池

    @Lazy // 添加 @Lazy 注解延迟加载
    @Resource
    private PostBloomFilterManager postBloomFilterManager; // 布隆过滤器管理器，用于快速判断数据是否存在

    @Resource
    private CacheManager cacheManager; // 注入缓存管理器

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    /**
     * 查询所有公开的社区帖子（带类型描述转换）
     *
     * @return 包含类型描述的帖子视图列表
     */
    @Override
    public List<CommunityPostsVO> selectAllCommunityPosts() {
        QueryWrapper<CommunityPosts> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_public", 1)
                .eq("is_delete", 0)
                .orderByDesc("is_top")
                .orderByDesc("create_time");

        List<CommunityPosts> posts = communityPostsMapper.selectList(queryWrapper);

        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = posts.stream()
                .map(CommunityPosts::getUserId)
                .collect(Collectors.toSet());

        final Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return posts.stream()
                .map(post -> {
                    CommunityPostsVO vo = convertToVO(post);
                    User user = userMap.get(post.getUserId());
                    if (user != null) {
                        vo.setNickname(user.getNickname());
                        vo.setAvatar(user.getAvatar());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }


    /**
     * 将帖子实体类转换为视图对象（含类型描述）
     *
     * @param post 帖子实体类
     * @return 包含类型描述的视图对象
     */
    private CommunityPostsVO convertToVO(CommunityPosts post) {
        CommunityPostsVO vo = new CommunityPostsVO();
        BeanCopyUtils.copy(post, vo); // 复制基础属性
        vo.setPostTypeDesc(getPostTypeDescription(post.getPostType())); // 添加类型描述
        return vo;
    }

    /**
     * 根据帖子类型枚举值获取描述信息
     *
     * @param postType 帖子类型枚举值
     * @return 类型描述字符串，未知类型返回"未知类型"
     */
    private String getPostTypeDescription(Integer postType) {
        CommunityPostsType type = CommunityPostsType.of(postType); // 通过枚举值获取枚举对象
        return type != null ? type.getDesc() : "未知类型"; // 安全返回描述或默认值
    }

    /**
     * 根据帖子ID查询详情（含缓存和布隆过滤器优化）
     *
     * @param postId 帖子ID
     * @return 帖子详情视图对象
     */
    @Override
    public CommunityPostsVO getCommunityPostsById(Long postId, Integer isPublic) {
        // 1. 参数校验
        ThrowUtils.throwIf(postId == null, ErrorCode.PARAMS_ERROR, "帖子ID不能为空");
        ThrowUtils.throwIf(isPublic == null || (isPublic != 0 && isPublic != 1),
                ErrorCode.PARAMS_ERROR, "isPublic 必须为 0 或 1");

        // 2. 构造缓存键（区分公开/私密）
        String cacheKey = postId + ":" + (isPublic == 1 ? "public" : "private");
        CommunityPostsVO cachedVO = (CommunityPostsVO) cacheManager.get(
                CommunityPostsConstant.POST_DETAIL_KEY_PREFIX,
                cacheKey
        );
        if (cachedVO != null) {
            return cachedVO;
        }

        // 3. 先尝试严格匹配查询
        QueryWrapper<CommunityPosts> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", postId)
                .eq("is_public", isPublic);
        CommunityPosts post = communityPostsMapper.selectOne(queryWrapper);

        // 4. 如果严格匹配查询失败，尝试只按ID查询（用于状态切换场景）
        if (post == null) {
            QueryWrapper<CommunityPosts> fallbackQuery = new QueryWrapper<>();
            fallbackQuery.eq("id", postId);
            post = communityPostsMapper.selectOne(fallbackQuery);

            if (post == null) {
                throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
            }

            // 如果查询到的帖子状态与期望的不一致，说明是状态切换场景
            if (post.getIsPublic() != isPublic) {
                // 清理所有相关缓存
                String publicCacheKey = postId + ":public";
                String privateCacheKey = postId + ":private";
                cacheManager.delete(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, publicCacheKey);
                cacheManager.delete(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, privateCacheKey);
            }
        }

        // 5. 更新布隆过滤器
        postBloomFilterManager.add(postId);

        // 6. 组装 VO
        User user = userMapper.selectById(post.getUserId());
        CommunityPostsVO postVO = new CommunityPostsVO();
        BeanCopyUtils.copy(post, postVO);
        postVO.setAvatar(user.getAvatar());
        postVO.setNickname(user.getNickname());
        postVO.setPostTypeDesc(getPostTypeDescription(post.getPostType()));

        // 7. 写入缓存（使用实际的 isPublic 值）
        String actualCacheKey = postId + ":" + (post.getIsPublic() == 1 ? "public" : "private");
        cacheManager.put(
                CommunityPostsConstant.POST_DETAIL_KEY_PREFIX,
                actualCacheKey,
                postVO,
                60 * 60  // 缓存1小时
        );

        return postVO;
    }


    /**
     * 获取热点帖子列表
     * 使用HeavyKeeper算法获取TopK热点
     */
    @Override
    public List<CommunityPostsVO> getHotPosts(int topN) {
        List<String> hotKeys = cacheManager.getHotKeys(topN); // 热点键格式为 "postId:public" 或 "postId:private"
        if (hotKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // 核心修复：从热点键中提取纯帖子ID（分割冒号，取前半部分）
        List<Long> postIds = hotKeys.stream()
                .map(key -> {
                    // 分割键（例如 "123:private" → ["123", "private"]）
                    String[] parts = key.split(":", 2); // 最多分割成两部分
                    if (parts.length == 0) {
                        log.warn("无效的热点键格式（空）: {}", key);
                        return null;
                    }
                    // 取分割后的第一部分（纯数字ID）
                    String idStr = parts[0].trim();
                    try {
                        return Long.parseLong(idStr); // 转换为数字ID
                    } catch (NumberFormatException e) {
                        log.warn("热点键无法转换为数字ID: {}", key, e);
                        return null; // 过滤无效ID
                    }
                })
                .filter(Objects::nonNull) // 移除转换失败的null值
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询数据库（保持原逻辑）
        List<CommunityPosts> posts = communityPostsMapper.selectBatchIds(postIds);

        // 使用LinkedHashMap保持热度排序
        Map<Long, CommunityPosts> postMap = posts.stream()
                .collect(Collectors.toMap(
                        CommunityPosts::getId,
                        post -> post,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        // 按热点顺序转换为VO
        return postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 点赞/取消点赞帖子（自动判断当前状态）
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 新的点赞状态（true=已点赞，false=未点赞）
     */
    @Override
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(postId == null || userId == null, ErrorCode.PARAMS_ERROR);

        // 布隆过滤器校验帖子是否存在
        if (!postBloomFilterManager.mightContain(postId)) {
            throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }

        // 查询帖子
        CommunityPosts post = communityPostsMapper.selectById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.COMMUNITY_POST_NOT_FOUND);

        // 查询用户当前点赞状态
        String redisKey = CommunityPostsConstant.USER_POSTS_KEY_PREFIX + userId;
        Boolean isLiked = redisTemplate.opsForHash().hasKey(redisKey, postId.toString());

        boolean newLikeState;

        if (Boolean.TRUE.equals(isLiked)) {
            // 当前已点赞 -> 执行取消点赞
            cancelLike(postId, userId);
            newLikeState = false;
        } else {
            // 当前未点赞 -> 执行点赞
            addLike(postId, userId);
            newLikeState = true;
        }

        return newLikeState;
    }

    /**
     * 根据用户ID查询帖子列表
     *
     * @param userId   用户ID
     * @param isPublic 是否只查询公开帖子(1:公开,0:所有)
     * @return 统一响应对象，包含帖子视图对象列表
     */
    @Override
    public List<CommunityPostsVO> listPostsByUserId(Long userId, Integer isPublic) {
        log.info("查询用户帖子参数 - userId: {}, isPublic: {}", userId, isPublic); // 添加这行
        // 参数校验
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");

        // 构建查询条件
        LambdaQueryWrapper<CommunityPosts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPosts::getUserId, userId)
                .eq(CommunityPosts::getIsDelete, 0);

        // 根据参数决定查询公开/私密/所有帖子
        if (isPublic != null) {
            queryWrapper.eq(CommunityPosts::getIsPublic, isPublic);
        }
        // 如果isPublic为null，则不添加isPublic条件，查询所有帖子

        // 按创建时间倒序排列
        queryWrapper.orderByDesc(CommunityPosts::getCreateTime);

        // 查询帖子列表
        List<CommunityPosts> posts = this.list(queryWrapper);
        log.info("实际查询到的帖子数量: {}", posts.size()); // 添加这行
        // 转换为VO对象
        return posts.stream().map(post -> {
            CommunityPostsVO vo = new CommunityPostsVO();
            BeanUtils.copyProperties(post, vo);

            // 查询用户信息
            User user = userService.getById(post.getUserId());
            if (user != null) {
                vo.setAvatar(user.getAvatar());
                vo.setNickname(user.getNickname());
            }

            return vo;
        }).collect(Collectors.toList());

    }

    /**
     * 删除帖子
     *
     * @param postId 帖子ID
     * @param userId 当前操作用户ID
     * @return 是否删除成功
     */
    @Override
    public boolean deletePost(Long postId, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");

        // 查询帖子信息
        CommunityPosts post = this.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.COMMUNITY_POST_NOT_FOUND);
        ThrowUtils.throwIf(post.getIsDelete() == 1, ErrorCode.OPERATION_ERROR, "帖子已被删除");

        // 查询用户信息
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOTFOUND);

        // 检查权限：管理员或帖子所有者
        boolean isAdmin = "admin".equals(user.getRole()) || "super_admin".equals(user.getRole());
        boolean isOwner = post.getUserId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该帖子");
        }

        // 执行删除（逻辑删除）
        CommunityPosts updatePost = new CommunityPosts();
        updatePost.setId(postId);
        updatePost.setIsDelete(1);
        updatePost.setUpdateTime(new Date());

        return this.updateById(updatePost);
    }

    /**
     * 根据关键词搜索帖子（按标题或内容）
     * @param keyword 搜索关键词
     * @return 匹配的帖子列表
     */
    @Override
    public List<CommunityPostsVO> searchPosts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 构建查询条件：标题或内容包含关键词，且为公开、未删除的帖子
        LambdaQueryWrapper<CommunityPosts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(CommunityPosts::getTitle, keyword)
                .or()
                .like(CommunityPosts::getContent, keyword))
                .eq(CommunityPosts::getIsPublic, 1)
                .eq(CommunityPosts::getIsDelete, 0)
                .orderByDesc(CommunityPosts::getIsTop)
                .orderByDesc(CommunityPosts::getCreateTime);

        List<CommunityPosts> posts = communityPostsMapper.selectList(queryWrapper);

        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        // 转换为视图对象
        Set<Long> userIds = posts.stream()
                .map(CommunityPosts::getUserId)
                .collect(Collectors.toSet());

        final Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<CommunityPostsVO> voList = posts.stream().map(post -> {
            CommunityPostsVO vo = new CommunityPostsVO();
            BeanUtils.copyProperties(post, vo);

            User user = userMap.get(post.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }

            // 设置类型描述（使用枚举的 of 方法根据数值获取类型）
            CommunityPostsType type = CommunityPostsType.of(post.getPostType());
            vo.setPostTypeDesc(type != null ? type.getDesc() : "未知");

            return vo;
        }).collect(Collectors.toList());

        return voList;
    }

    @Override
    public List<CommunityPostsVO> getPostsByFollowingIds(List<Long> followingIds) {
        if (followingIds == null || followingIds.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<CommunityPosts> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", followingIds)
                .eq("is_public", 1)
                .eq("is_delete", 0)
                .orderByDesc("is_top")
                .orderByDesc("create_time");

        List<CommunityPosts> posts = communityPostsMapper.selectList(queryWrapper);

        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = posts.stream()
                .map(CommunityPosts::getUserId)
                .collect(Collectors.toSet());

        final Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return posts.stream()
                .map(post -> {
                    CommunityPostsVO vo = convertToVO(post);
                    User user = userMap.get(post.getUserId());
                    if (user != null) {
                        vo.setNickname(user.getNickname());
                        vo.setAvatar(user.getAvatar());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 执行点赞操作
     */
    private void addLike(Long postId, Long userId) {
        // 查询用户对该帖子的历史点赞记录
        QueryWrapper<PostLikes> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", postId).eq("user_id", userId);
        PostLikes existingLike = postLikesMapper.selectOne(queryWrapper);

        if (existingLike != null) {
            // 恢复已取消的点赞
            existingLike.setIsDelete(0);
            existingLike.setUpdateTime(new Date());
            postLikesMapper.updateById(existingLike);
        } else {
            // 新增点赞记录
            PostLikes like = new PostLikes();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreateTime(new Date());
            like.setUpdateTime(new Date());
            like.setIsDelete(0);
            postLikesMapper.insert(like);
        }

        // 异步更新缓存和点赞数
        asyncUpdateLikeStatus(postId, userId, true);
    }

    /**
     * 执行取消点赞操作
     */
    private void cancelLike(Long postId, Long userId) {
        // 查询有效的点赞记录
        QueryWrapper<PostLikes> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", postId)
                .eq("user_id", userId)
                .eq("is_delete", 0);
        PostLikes like = postLikesMapper.selectOne(queryWrapper);

        if (like != null) {
            // 标记为已取消
            like.setIsDelete(1);
            like.setUpdateTime(new Date());
            postLikesMapper.updateById(like);

            // 异步更新缓存和点赞数
            asyncUpdateLikeStatus(postId, userId, false);
        }
    }

    /**
     * 异步更新点赞状态和缓存
     */
    private void asyncUpdateLikeStatus(Long postId, Long userId, boolean isLike) {
        redisExecutor.execute(() -> {
            String redisKey = CommunityPostsConstant.USER_POSTS_KEY_PREFIX + userId;

            if (isLike) { // 点赞操作
                // 更新用户点赞记录（使用Hash结构存储用户点赞的帖子ID）
                redisTemplate.opsForHash().put(redisKey, postId.toString(), true);
                redisTemplate.expire(redisKey, CommunityPostsConstant.USER_POSTS_LIKE_EXPIRE_TIME, TimeUnit.SECONDS);

                // 记录热点（用于热门帖子排序）
                cacheManager.get(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, postId.toString());

                // 原子更新帖子点赞数（+1）
                communityPostsMapper.update(
                        null,
                        new UpdateWrapper<CommunityPosts>()
                                .setSql("like_count = like_count + 1")
                                .eq("id", postId)
                );
            } else { // 取消点赞操作
                // 删除用户点赞记录
                redisTemplate.opsForHash().delete(redisKey, postId.toString());

                // 原子更新帖子点赞数（-1）
                communityPostsMapper.update(
                        null,
                        new UpdateWrapper<CommunityPosts>()
                                .setSql("like_count = like_count - 1")
                                .eq("id", postId)
                );
            }
            
            // 删除帖子详情缓存，确保下次查询时获取最新的点赞数
            // 需要删除 public 和 private 两种缓存（因为同一个帖子可能在不同场景下查询）
            String publicCacheKey = postId + ":public";
            String privateCacheKey = postId + ":private";
            cacheManager.delete(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, publicCacheKey);
            cacheManager.delete(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, privateCacheKey);
        });
    }



//    /**
//     * 点赞帖子（支持重复点赞自动判断，含事务和缓存更新）
//     *
//     * @param postId 帖子ID
//     * @param userId 用户ID
//     * @return 点赞操作是否成功
//     */
//    @Override
//    @Transactional // 声明事务保证数据库操作一致性
//    public boolean likeCommunityPosts(Long postId, Long userId) {
//        // 布隆过滤器提前过滤无效帖子ID（减少数据库查询压力）
//        if (!postBloomFilterManager.mightContain(postId)) {
//            ThrowUtils.throwIf(true, ErrorCode.COMMUNITY_POST_NOT_FOUND); // 帖子不存在则抛出异常
//        }
//
//        // 查询帖子是否存在（双重校验，确保数据一致性）
//        CommunityPosts post = communityPostsMapper.selectById(postId);
//        ThrowUtils.throwIf(post == null, ErrorCode.COMMUNITY_POST_NOT_FOUND); // 再次校验帖子存在
//
//        // 查询用户对该帖子的历史点赞记录
//        QueryWrapper<PostLikes> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("post_id", postId).eq("user_id", userId);
//        PostLikes existingLike = postLikesMapper.selectOne(queryWrapper);
//
//        boolean success = false;
//
//        if (existingLike != null) {
//            if (existingLike.getIsDelete() == 0) {
//                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "请勿重复点赞");
//            } else {
//                existingLike.setIsDelete(0);
//                existingLike.setUpdateTime(new Date());
//                success = postLikesMapper.updateById(existingLike) > 0;
//            }
//        } else {
//            PostLikes like = new PostLikes();
//            like.setPostId(postId);
//            like.setUserId(userId);
//            like.setCreateTime(new Date());
//            like.setUpdateTime(new Date());
//            like.setIsDelete(0);
//            success = postLikesMapper.insert(like) > 0;
//        }
//
//        // 异步更新缓存和热点统计
//        // 修改后（新增点赞数+1）
//        if (success) {
//            redisExecutor.execute(() -> {
//                // 原有逻辑
//                String redisKey = CommunityPostsConstant.USER_POSTS_KEY_PREFIX + userId;
//                redisTemplate.opsForHash().put(redisKey, postId.toString(), true);
//                redisTemplate.expire(redisKey, CommunityPostsConstant.USER_POSTS_LIKE_EXPIRE_TIME, TimeUnit.SECONDS);
//                cacheManager.get(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, postId.toString());
//
//                // **新增：原子更新帖子点赞数（+1）**
//                communityPostsMapper.update(
//                        null,
//                        new UpdateWrapper<CommunityPosts>()
//                                .setSql("like_count = like_count + 1")
//                                .eq("id", postId)
//                );
//            });
//        }
//
//        return success;
//    }
//
//    /**
//     * 取消点赞帖子（含事务和缓存更新）
//     *
//     * @param postId 帖子ID
//     * @param userId 用户ID
//     * @return 取消操作是否成功
//     */
//    @Override
//    public boolean unlikeCommunityPosts(Long postId, Long userId) {
//        // 使用布隆过滤器快速判断帖子是否可能存在（减少无效查询）
//        if (!postBloomFilterManager.mightContain(postId)) {
//            ThrowUtils.throwIf(true, ErrorCode.COMMUNITY_POST_NOT_FOUND); // 帖子不存在则抛出异常
//        }
//
//        // 查询有效的点赞记录（is_delete=0表示未取消）
//        QueryWrapper<PostLikes> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("post_id", postId)
//                .eq("user_id", userId)
//                .eq("is_delete", 0); // 仅查询未取消的点赞记录
//        PostLikes like = postLikesMapper.selectOne(queryWrapper);
//        ThrowUtils.throwIf(like == null, ErrorCode.OPERATION_ERROR, "请勿重复取消点赞"); // 校验存在有效点赞记录
//
//        // 标记为取消点赞（软删除）
//        like.setIsDelete(1); // 标记为已取消
//        like.setUpdateTime(new Date()); // 更新操作时间
//        int rows = postLikesMapper.updateById(like); // 执行更新操作
//
//        // 异步删除Redis缓存中的点赞记录
//        // 修改后（新增点赞数-1）
//        if (rows > 0) {
//            redisExecutor.execute(() -> {
//                // 原有逻辑
//                String redisKey = CommunityPostsConstant.USER_POSTS_KEY_PREFIX + userId;
//                redisTemplate.opsForHash().delete(redisKey, postId.toString());
//
//                // **新增：原子更新帖子点赞数（-1）**
//                communityPostsMapper.update(
//                        null,
//                        new UpdateWrapper<CommunityPosts>()
//                                .setSql("like_count = like_count - 1")
//                                .eq("id", postId)
//                );
//            });
//        }
//
//        return rows > 0;
//    }

    /**
     * 判断用户是否点赞过某帖子（优先查询Redis缓存）
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 是否点赞过（存在误判可能，需结合数据库最终结果）
     */
    @Override
    public Boolean hashCommunityPosts(Long postId, Long userId) {
        // 使用布隆过滤器快速排除不存在的帖子（减少Redis查询压力）
        if (!postBloomFilterManager.mightContain(postId)) {
            return false; // 帖子不存在则直接返回未点赞
        }

        String redisKey = CommunityPostsConstant.USER_POSTS_KEY_PREFIX + userId;
        // 使用Hash结构的hasKey方法判断是否存在点赞记录（O(1)复杂度）
        return redisTemplate.opsForHash().hasKey(redisKey, postId.toString());
    }

    /**
     * 批量判断用户是否点赞多个帖子（含布隆过滤器预过滤和批量查询优化）
     *
     * @param userId  用户ID
     * @param postIds 帖子ID列表
     * @return 帖子ID到是否点赞的映射（不存在的帖子自动标记为false）
     */
    @Override
    public Map<Long, Boolean> hasLikedPostsBatch(Long userId, List<Long> postIds) {
        // 对帖子ID列表进行布隆过滤器预过滤，仅保留可能存在的ID
        List<Long> validPostIds = postIds.stream()
                .filter(postBloomFilterManager::mightContain) // 过滤掉不可能存在的帖子ID
                .collect(Collectors.toList());

        String redisKey = CommunityPostsConstant.USER_POSTS_KEY_PREFIX + userId;
        // 批量查询Redis中的点赞记录（减少网络IO次数）
        List<Object> results = redisTemplate.opsForHash().multiGet(
                redisKey,
                validPostIds.stream().map(Object::toString).collect(Collectors.toList()) // 转换为字符串列表
        );

        // 构建结果映射
        Map<Long, Boolean> map = new HashMap<>();
        for (int i = 0; i < validPostIds.size(); i++) {
            map.put(validPostIds.get(i), results.get(i) != null); // null表示未点赞，非null表示已点赞
        }

        // 处理被布隆过滤器过滤掉的帖子ID（直接标记为false）
        postIds.stream()
                .filter(id -> !validPostIds.contains(id))
                .forEach(id -> map.put(id, false));

        return map;
    }

    /**
     * 发布帖子并处理缓存与布隆过滤器。
     *
     * @param post 待发布的帖子实体，需包含标题、内容等基础信息
     * @return 发布成功后生成的帖子 ID
     * @throws BusinessException 若帖子插入数据库失败
     */
    @Override
    @Transactional
    public Long publishPost(CommunityPosts post) {
        // 初始化帖子字段
        post.setCreateTime(new Date());
        post.setUpdateTime(new Date());
        post.setIsDelete(0);
        post.setIsPublic(post.getIsPublic()); // 默认为公开
        post.setIsTop(post.getIsTop() != null ? post.getIsTop() : 0); // 默认为非置顶

        // 插入数据库
        int result = communityPostsMapper.insert(post);
        ThrowUtils.throwIf(result <= 0, ErrorCode.ADD_POST_ERROR);

        Long postId = post.getId();

        // 加入布隆过滤器
        postBloomFilterManager.add(postId);

        // ✅ 使用 CacheManager 写入缓存（自动处理本地和 Redis，记录热点）
        CommunityPostsVO vo = new CommunityPostsVO();
        BeanCopyUtils.copy(post, vo);
        vo.setPostTypeDesc(getPostTypeDescription(post.getPostType()));

        cacheManager.put(
                CommunityPostsConstant.POST_DETAIL_KEY_PREFIX,
                postId.toString(),
                vo,
                CommunityPostsConstant.POST_CACHE_EXPIRE_TIME * 60  // 转换为秒（原单位可能为分钟）
        );

        return postId;
    }

    /**
     * 修改帖子并返回修改后的视图对象。
     *
     * @param updatedPost 包含修改字段的帖子对象，必须携带 ID
     * @return 最新的帖子视图对象（CommunityPostsVO）
     * @throws BusinessException 若帖子不存在或数据库更新失败
     */
    @Override
    @Transactional
    public CommunityPostsVO updatePostAndReturnVO(CommunityPosts updatedPost) {
        CommunityPosts originalPost = communityPostsMapper.selectById(updatedPost.getId());
        ThrowUtils.throwIf(originalPost == null, ErrorCode.COMMUNITY_POST_NOT_FOUND);

        // 保存原始的 isPublic 值，用于清理旧缓存
        Integer originalIsPublic = originalPost.getIsPublic();

        updatedPost.setUpdateTime(new Date());
        int rows = communityPostsMapper.updateById(updatedPost);

        if (rows > 0) {
            Long postId = updatedPost.getId();
            Integer newIsPublic = updatedPost.getIsPublic();

            // 清理所有相关的缓存（包括公开和私密的缓存）
            String publicCacheKey = postId + ":public";
            String privateCacheKey = postId + ":private";

            cacheManager.delete(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, publicCacheKey);
            cacheManager.delete(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, privateCacheKey);

            // 重新查询并返回最新视图（使用新的 isPublic 值）
            return getCommunityPostsById(postId, newIsPublic);
        } else {
            throw new BusinessException(ErrorCode.UPDATE_POST_ERROR);
        }
    }


    /**
     * 根据帖子ID查询帖子详情（管理员使用）
     *
     * @param postId 帖子ID，不能为空
     * @return 帖子详情视图对象，包含完整的帖子信息和相关统计数据
     */
    @Override
    public CommunityPostsVO getCommunityPostsByIdAdmin(Long postId) {
        // Step 1: 使用布隆过滤器快速过滤无效ID（存在误判可能，但不会漏判）
        if (!postBloomFilterManager.mightContain(postId)) {
            ThrowUtils.throwIf(true, ErrorCode.COMMUNITY_POST_NOT_FOUND); // 不存在则抛出异常
        }

        ThrowUtils.throwIf(postId == null, ErrorCode.COMMUNITY_POST_NOT_FOUND); // 校验ID非空

        // Step 2: 优先查询Redis缓存（热点数据快速响应）
        String redisKey = CommunityPostsConstant.POST_DETAIL_KEY_PREFIX + postId;
        CommunityPostsVO cachedVO = (CommunityPostsVO) redisTemplate.opsForValue().get(redisKey);
        if (cachedVO != null) {
            // 命中缓存时增加帖子热度评分（异步更新，避免阻塞）
            redisTemplate.opsForZSet().incrementScore(CommunityPostsConstant.POST_HOT_SCORE_KEY, postId, 1);
            return cachedVO; // 直接返回缓存数据
        }

        // Step 3: 缓存未命中时查询数据库
        CommunityPosts post = communityPostsMapper.selectById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.COMMUNITY_POST_NOT_FOUND); // 校验数据库记录存在

        CommunityPostsVO postVO = new CommunityPostsVO();
        BeanCopyUtils.copy(post, postVO); // 复制实体类属性到视图对象

        // Step 4: 将查询结果写入Redis缓存（设置过期时间，避免内存占用过高）
        redisTemplate.opsForValue().set(redisKey, postVO, CommunityPostsConstant.POST_CACHE_EXPIRE_TIME, TimeUnit.MINUTES);

        // Step 5: 记录帖子访问热度（使用ZSet实现排序，分值越高越热门）
        redisTemplate.opsForZSet().incrementScore(CommunityPostsConstant.POST_HOT_SCORE_KEY, postId, 1);

        return postVO;
    }


    /**
     * 定时任务：清理冷门帖子缓存（每天凌晨执行）
     * 保留热度排名前100的帖子缓存，删除其余过期缓存
     */
    @Scheduled(cron = "0 0 * * * ?") // 每天0点执行（cron表达式：秒 分 时 日 月 周）
    public void evictColdPostCache() {
        // 获取热度排名前100的帖子ID（ZSet按分值降序排列）
        Set<Object> hotPostIds = redisTemplate.opsForZSet().reverseRange(CommunityPostsConstant.POST_HOT_SCORE_KEY, 0, 99);

        // 获取所有帖子详情缓存的key（使用Redis的keys命令，注意生产环境可能影响性能）
        Set<String> keys = redisTemplate.keys(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                // 解析帖子ID
                String idStr = key.replace(CommunityPostsConstant.POST_DETAIL_KEY_PREFIX, "");
                Long postId = Long.parseLong(idStr);

                // 判断是否属于冷门帖子（不在热度Top100中）
                if (!hotPostIds.contains(postId)) {
                    redisTemplate.delete(key); // 删除冷门帖子缓存
                }
            }
        }
    }

}