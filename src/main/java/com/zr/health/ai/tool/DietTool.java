package com.zr.health.ai.tool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zr.health.context.BaseContext;
import com.zr.health.model.dto.food.DietFoodItemDTO;
import com.zr.health.model.dto.food.DietRecordAddDTO;
import com.zr.health.model.entity.FoodLibrary;
import com.zr.health.service.DietRecordsService;
import com.zr.health.service.FoodLibraryService;
import com.zr.health.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
    @Tool(description = """
            添加饮食记录到数据库。
            当用户提到吃了什么、喝了什么、某餐吃了什么、要求记录饮食、计算热量时调用此工具。
            触发场景举例：「我早餐吃了…」「记录一下午餐」「今天吃了…」「帮我记晚饭」「刚才喝了…」「加餐吃了…」。
            用户必须明确或可推断餐次类型（早餐/午餐/晚餐/加餐）和具体食物名称及用量。
            """)
    public String addDietRecord(
            @ToolParam(description = "餐次类型，必须是以下之一：早餐、午餐、晚餐、加餐。如用户未明确说明，根据时间和食物内容推断", required = true) String mealType,
            @ToolParam(description = """
                    食物项列表。多个食物用分号(;)分隔，每个食物格式：食物名称:食用量:单位:备注。
                    单位必须是 'g' 或 '份'，备注可选。
                    用户通常不会提供精确克数（如"吃了两个鸡蛋""喝了一杯牛奶""吃了一碗米饭"），
                    请根据常识自动估算合理克数，参考值：
                    - 1个鸡蛋 ≈ 50g、1碗米饭 ≈ 150g、1杯牛奶 ≈ 250g、1个苹果 ≈ 200g
                    - 1片面包 ≈ 30g、1根香蕉 ≈ 120g、1个馒头 ≈ 100g、1碗粥 ≈ 300g
                    - 对于明确可按'份'计数的食物（如鸡蛋、包子），优先使用'份'作为单位
                    示例：'鸡蛋:2:份:水煮;牛奶:250:g:全脂;馒头:1:份'。
                    注意：食物名称必须与食物库中已有名称一致，相近的请自行匹配。
                    """, required = true) String foodItems
     ) {

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
        log.info("业务处理中获取的用户ID：{}", BaseContext.getCurrentId());

        if (userId == null) {
            log.error("用户ID为null，可能导致数据库插入失败");
            return "操作失败：用户会话已过期，请重新登录";
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

