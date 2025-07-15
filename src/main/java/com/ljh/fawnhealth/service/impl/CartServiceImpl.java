package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.mapper.CartMapper;
import com.ljh.fawnhealth.model.entity.Cart;
import com.ljh.fawnhealth.service.CartService;
import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【cart(购物车表)】的数据库操作Service实现
* @createDate 2025-07-14 23:01:15
*/
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart>
    implements CartService {

}




