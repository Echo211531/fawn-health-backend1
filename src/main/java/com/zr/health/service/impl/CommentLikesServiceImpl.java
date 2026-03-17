package com.zr.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.mapper.CommentLikesMapper;
import com.zr.health.model.entity.CommentLikes;
import com.zr.health.service.CommentLikesService;
import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【comment_likes(评论点赞表)】的数据库操作Service实现
* @createDate 2025-06-01 22:14:10
*/
@Service
public class CommentLikesServiceImpl extends ServiceImpl<CommentLikesMapper, CommentLikes>
    implements CommentLikesService {

}




