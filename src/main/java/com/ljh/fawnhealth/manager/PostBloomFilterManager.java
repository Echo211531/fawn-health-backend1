package com.ljh.fawnhealth.manager;

import com.ljh.fawnhealth.model.vo.communityPosts.CommunityPostsVO;
import com.ljh.fawnhealth.service.CommunityPostsService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostBloomFilterManager implements InitializingBean {

    private static final String POST_ID_BLOOM_FILTER = "community:bloom:postId";

    @Resource
    private RedissonClient redissonClient;

    @Lazy
    @Resource
    private CommunityPostsService communityPostsService; // 注入Service获取所有postId

    @Override
    public void afterPropertiesSet() throws Exception {
        List<Long> allPostIds = new ArrayList<>();
        // 应用启动时自动调用，完成布隆过滤器初始化
        List<CommunityPostsVO> list = communityPostsService.selectAllCommunityPosts();
        for (CommunityPostsVO communityPostsVO : list) {
            allPostIds.add(communityPostsVO.getId());
        }

        if (!allPostIds.isEmpty()) {
            initBloomFilter(allPostIds); // 调用现有初始化方法
        }
    }

    public boolean mightContain(Long postId) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(POST_ID_BLOOM_FILTER);
        return bloomFilter.contains(postId);
    }

    public void addPostId(Long postId) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(POST_ID_BLOOM_FILTER);
        bloomFilter.add(postId);
    }

    public void initBloomFilter(List<Long> allPostIds) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(POST_ID_BLOOM_FILTER);
        bloomFilter.tryInit(100_000L, 0.01); // 初始化参数
        allPostIds.forEach(bloomFilter::add); // 批量添加ID
        System.out.println("布隆过滤器初始化：添加 " + allPostIds.size() + " 个帖子ID");
    }

    public void add(Long postId) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(POST_ID_BLOOM_FILTER);
        bloomFilter.add(postId);
    }

}
