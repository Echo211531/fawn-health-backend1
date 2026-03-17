package com.zr.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zr.health.mapper.DietFoodItemsMapper;
import com.zr.health.model.entity.DietFoodItems;
import com.zr.health.service.DietFoodItemsService;

import org.springframework.stereotype.Service;

/**
* @author 27105
* @description 针对表【diet_food_items(饮食记录-食物项表)】的数据库操作Service实现
* @createDate 2025-05-25 18:10:48
*/
@Service
public class DietFoodItemsServiceImpl extends ServiceImpl<DietFoodItemsMapper, DietFoodItems>
    implements DietFoodItemsService {

}




