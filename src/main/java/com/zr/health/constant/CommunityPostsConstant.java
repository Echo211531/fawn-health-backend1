package com.zr.health.constant;

public interface CommunityPostsConstant {

    // Redis 缓存帖子详情 key 前缀，例如：community:post:detail:123
    String POST_DETAIL_KEY_PREFIX = "community:post:detail:";

    // 帖子缓存时间（单位：分钟）
    long POST_CACHE_EXPIRE_TIME = 10L;

    // Redis ZSet 记录帖子的热度评分
    String POST_HOT_SCORE_KEY = "community:post:hot:score";

    // 用户点赞帖子 Hash 的 Redis key 前缀，例如：user:post:like:456
    String USER_POSTS_KEY_PREFIX = "user:post:like:";

    // 用户点赞过期时间（7天，单位：秒）
    long USER_POSTS_LIKE_EXPIRE_TIME = 7 * 24 * 60 * 60L;

    String TEMP_THUMB_KEY_PREFIX = "community:post:%s";

}
