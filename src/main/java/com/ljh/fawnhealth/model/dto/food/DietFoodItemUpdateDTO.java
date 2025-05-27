package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 饮食记录项更新请求DTO
 * 用于更新用户饮食记录中的具体食物项信息
 */
@Data
public class DietFoodItemUpdateDTO {
    /**
     * 食物ID，必填
     * 对应数据库中diet_food_item表的主键
     */
    private Long foodId;

    /**
     * 食用量，必填
     * 表示摄入该食物的具体数量
     */
    private BigDecimal amount;

    /**
     * 单位，必填
     * 例如：g(克)、ml(毫升)、份、个等
     */
    private String unit;

    /**
     * 备注信息，选填
     * 可记录烹饪方式、特殊说明等
     */
    private String note;
}