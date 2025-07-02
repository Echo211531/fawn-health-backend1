package com.ljh.fawnhealth.ai.tools;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljh.fawnhealth.context.BaseContext;
import com.ljh.fawnhealth.model.dto.food.DietFoodItemDTO;
import com.ljh.fawnhealth.model.dto.food.DietRecordAddDTO;
import com.ljh.fawnhealth.model.entity.FoodLibrary;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.service.DietRecordsService;
import com.ljh.fawnhealth.service.FoodLibraryService;
import com.ljh.fawnhealth.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class DietTool {

    @Resource
    private DietRecordsService dietRecordsService;
    @Resource
    private FoodLibraryService foodLibraryService;
    @Resource
    private UserService userService;

    /**
     * 添加饮食记录工具方法
     * 1. 验证食物是否存在于数据库
     * 2. 构建饮食记录DTO并保存
     * 3. 使用事务保证数据一致性
     */
    //@Transactional(rollbackFor = Exception.class)
    @Tool( description = "添加饮食记录到数据库，用户必须指定餐次类型（早餐/午餐/晚餐/加餐）和食物列表。")
    public String addDietRecord(
            @ToolParam(description = "餐次类型，必须是：早餐、午餐、晚餐或加餐", required = true) String mealType,
            @ToolParam(description = "食物项列表(提供名称、食用量、单位（g或份）和可选备注)，格式：'食物名称1:食用量1:单位1:备注1;食物名称2:食用量2:单位2:备注2'", required = true) String foodItems
    ,HttpServletRequest request) {

        // 验证并转换餐次类型
        Integer mealTypeCode = validateMealType(mealType);
        if (mealTypeCode == null) {
            return "餐次类型无效！必须是：早餐、午餐、晚餐或加餐";
        }

        // 解析食物项字符串并验证
        List<DietFoodItemDTO> validItems = parseAndValidateFoodItems(foodItems);
        if (validItems == null || validItems.isEmpty()) {
            return "食物项格式错误或存在无效食物！请按格式提供：'食物名称:食用量:单位:备注'，多组用分号分隔。例如：'牛奶:250:g:全脂;鸡蛋:1:份:水煮蛋'";
        }
        Long userId = BaseContext.getCurrentId();
        if( userId == null){
            userId=userService.getLoginUser(request).getId();
        }

        // 构建并保存饮食记录
        DietRecordAddDTO recordDTO = new DietRecordAddDTO();
        recordDTO.setUserId(userId);
        recordDTO.setMealType(mealTypeCode);
        recordDTO.setFoodItems(validItems);

        try {
            // 调用服务方法并获取总热量
            BigDecimal totalCalories = dietRecordsService.addDietRecord(recordDTO);
            return String.format("饮食记录添加成功！总热量：%.2f 千卡", totalCalories);
        } catch (Exception e) {
            // 记录错误日志
            e.printStackTrace();
            return "饮食记录保存失败: " + e.getMessage();
        }
    }


    /**
     * 验证并转换餐次类型
     */
    private Integer validateMealType(String mealType) {
        return switch (mealType) {
            case "早餐" -> 1;
            case "午餐" -> 2;
            case "晚餐" -> 3;
            case "加餐" -> 4;
            default -> null;
        };
    }



    /**
     * 解析并验证食物项字符串（一步完成）
     */
    private List<DietFoodItemDTO> parseAndValidateFoodItems(String foodItems) {
        List<DietFoodItemDTO> validItems = new ArrayList<>();
        String[] itemArray = foodItems.split(";");

        for (String itemStr : itemArray) {
            String[] parts = itemStr.split(":", 4); // 分成4部分

            // 格式验证
            if (parts.length < 3)
                return null;
            String foodName = parts[0].trim();
            String amountStr = parts[1].trim();
            String unit = parts[2].trim();
            String note = parts.length > 3 ? parts[3].trim() : "";

            // 单位验证
            if (!"g".equalsIgnoreCase(unit) && !"份".equals(unit)) {
                return null;
            }

            // 用量验证
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0)
                    return null;
            } catch (Exception e) {
                return null;
            }

            // 查询食物库
            LambdaQueryWrapper<FoodLibrary> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FoodLibrary::getName, foodName);   // 使用正确的字段名
            FoodLibrary food = foodLibraryService.getOne(wrapper);

            if (food == null) {
                return null; // 食物不存在
            }

            // 构建有效DTO
            DietFoodItemDTO itemDTO = new DietFoodItemDTO();
            itemDTO.setFoodId(food.getId());   // 使用正确的ID字段
            itemDTO.setAmount(amount);
            itemDTO.setUnit(unit);
            itemDTO.setNote(note);

            validItems.add(itemDTO);
        }
        return validItems;
    }
}

