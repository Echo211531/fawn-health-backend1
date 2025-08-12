package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;

import java.util.Date;

@Data
public class DietRecordDTO {
    private Date recordDate;
    private Integer mealType;
    private NutritionData nutrition;
}