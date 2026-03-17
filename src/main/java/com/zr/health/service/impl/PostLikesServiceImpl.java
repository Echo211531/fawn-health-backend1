package com.zr.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.mapper.PostLikesMapper;
import com.zr.health.model.entity.PostLikes;
import com.zr.health.service.PostLikesService;
import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【post_likes(帖子点赞表)】的数据库操作Service实现
* @createDate 2025-05-17 17:05:23
*/
@Service
public class PostLikesServiceImpl extends ServiceImpl<PostLikesMapper, PostLikes>
    implements PostLikesService {

}




