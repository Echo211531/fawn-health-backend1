package com.zr.health.model.dto.user;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 用户体重和目标体重DTO
 */
@Data
public class WeightDTO {

    /**
     * ID
     */
    private Long userId;

    /**
     * 体重(kg)
     */
    private BigDecimal weight;


    /**
     * 目标体重(kg)
     */
    private BigDecimal targetWeight;

    /**
     * 目标天数
     */
    private Integer periodDays;
}

