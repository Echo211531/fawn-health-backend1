package com.ljh.fawnhealth.utils;

import com.ljh.fawnhealth.model.entity.DietRecords;
import com.ljh.fawnhealth.model.vo.food.DietRecordSimpleVO;
import com.ljh.fawnhealth.model.dto.food.DietFoodItemDTO;
import com.ljh.fawnhealth.model.enums.rule.NutritionType;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;

/**
 * 营养计算工具类
 * 用于计算用户饮食记录的营养数据
 */
public class NutritionCalculatorUtil {

    /**
     * 计算总营养摄入量（通用方法）
     */
    public static Map<NutritionType, BigDecimal> calculateTotalNutrition(List<?> records) {
        Map<NutritionType, BigDecimal> totals = new EnumMap<>(NutritionType.class);
        for (NutritionType type : NutritionType.values()) {
            totals.put(type, BigDecimal.ZERO);
        }
        if (records == null || records.isEmpty()) {
            return totals;
        }

        if (records.get(0) instanceof DietRecords) {
            return calculateTotalNutritionFromDietRecords((List<DietRecords>) records);
        } else if (records.get(0) instanceof DietRecordSimpleVO) {
            return calculateTotalNutritionFromSimpleVO((List<DietRecordSimpleVO>) records);
        }
        return totals;
    }

    /**
     * 从DietRecords计算营养总量（目前仅汇总总热量字段）
     */
    private static Map<NutritionType, BigDecimal> calculateTotalNutritionFromDietRecords(List<DietRecords> records) {
        Map<NutritionType, BigDecimal> totals = new EnumMap<>(NutritionType.class);
        for (NutritionType type : NutritionType.values()) {
            totals.put(type, BigDecimal.ZERO);
        }
        for (DietRecords record : records) {
            if (record.getTotalCalories() != null) {
                totals.put(NutritionType.CALORIES,
                        totals.get(NutritionType.CALORIES).add(record.getTotalCalories()));
            }
        }
        return totals;
    }

    /**
     * 从DietRecordSimpleVO计算营养总量：直接汇总每个食物项的营养字段
     */
    private static Map<NutritionType, BigDecimal> calculateTotalNutritionFromSimpleVO(
            List<DietRecordSimpleVO> records) {
        Map<NutritionType, BigDecimal> totals = new EnumMap<>(NutritionType.class);
        for (NutritionType type : NutritionType.values()) {
            totals.put(type, BigDecimal.ZERO);
        }
        for (DietRecordSimpleVO record : records) {
            List<DietFoodItemDTO> items = record.getFoodItems();
            if (items == null || items.isEmpty())
                continue;
            for (DietFoodItemDTO food : items) {
                totals.put(NutritionType.CALORIES, totals.get(NutritionType.CALORIES)
                        .add(defaultIfNull(food.getCalories())));
                totals.put(NutritionType.PROTEIN, totals.get(NutritionType.PROTEIN)
                        .add(defaultIfNull(food.getProtein())));
                totals.put(NutritionType.FAT, totals.get(NutritionType.FAT)
                        .add(defaultIfNull(food.getFat())));
                totals.put(NutritionType.CARBOHYDRATE, totals.get(NutritionType.CARBOHYDRATE)
                        .add(defaultIfNull(food.getCarbohydrate())));
            }
        }
        return totals;
    }

    /**
     * 计算连续天数指标（通用方法）
     */
    public static Map<String, Integer> calculateConsecutiveDays(List<?> records) {
        if (records == null || records.isEmpty()) {
            return new HashMap<>();
        }
        if (records.get(0) instanceof DietRecords) {
            return calculateConsecutiveDaysFromDietRecords((List<DietRecords>) records);
        } else if (records.get(0) instanceof DietRecordSimpleVO) {
            return calculateConsecutiveDaysFromSimpleVO((List<DietRecordSimpleVO>) records);
        }
        return new HashMap<>();
    }

    /**
     * 从DietRecords计算连续天数（基于总热量阈值的近似替代）
     */
    private static Map<String, Integer> calculateConsecutiveDaysFromDietRecords(List<DietRecords> records) {
        Map<String, Integer> result = new HashMap<>();
        records.sort(Comparator.comparing(DietRecords::getRecordDate).reversed());
        int lowVegDays = calculateConsecutiveDietRecords(records,
                r -> r.getTotalCalories() != null && r.getTotalCalories().compareTo(new BigDecimal("800")) < 0);
        result.put("lowVegetable", lowVegDays);
        int highFatDays = calculateConsecutiveDietRecords(records,
                r -> r.getTotalCalories() != null && r.getTotalCalories().compareTo(new BigDecimal("1200")) > 0);
        result.put("highFat", highFatDays);
        int highCarbDays = calculateConsecutiveDietRecords(records,
                r -> r.getTotalCalories() != null && r.getTotalCalories().compareTo(new BigDecimal("1000")) > 0);
        result.put("highCarb", highCarbDays);
        return result;
    }

    /**
     * 从DietRecordSimpleVO计算连续天数：
     * - lowVegetable: 当天蔬菜摄入总量 < 200g 记为1天
     * - highFat: 当天脂肪总量 > 50g 记为1天
     * - highCarb: 当天碳水总量 > 300g 记为1天
     */
    private static Map<String, Integer> calculateConsecutiveDaysFromSimpleVO(List<DietRecordSimpleVO> records) {
        Map<String, Integer> result = new HashMap<>();
        records.sort(Comparator.comparing(DietRecordSimpleVO::getRecordDate).reversed());
        int lowVegDays = calculateConsecutiveSimpleVO(records, NutritionCalculatorUtil::isLowVegetableDay);
        int highFatDays = calculateConsecutiveSimpleVO(records,
                r -> dayTotalFat(r).compareTo(new BigDecimal("50")) > 0);
        int highCarbDays = calculateConsecutiveSimpleVO(records,
                r -> dayTotalCarb(r).compareTo(new BigDecimal("300")) > 0);
        result.put("lowVegetable", lowVegDays);
        result.put("highFat", highFatDays);
        result.put("highCarb", highCarbDays);
        return result;
    }

    private static boolean isLowVegetableDay(DietRecordSimpleVO record) {
        BigDecimal vegetables = dayVegetableAmount(record);
        return vegetables.compareTo(new BigDecimal("200")) < 0;
    }

    private static BigDecimal dayVegetableAmount(DietRecordSimpleVO record) {
        BigDecimal sum = BigDecimal.ZERO;
        List<DietFoodItemDTO> items = record.getFoodItems();
        if (items == null)
            return BigDecimal.ZERO;
        for (DietFoodItemDTO item : items) {
            String category = item.getCategoryName();
            if (category != null && category.contains("蔬菜")) {
                sum = sum.add(defaultIfNull(item.getAmount()));
            }
        }
        return sum;
    }

    private static BigDecimal dayTotalCalories(DietRecordSimpleVO record) {
        BigDecimal sum = BigDecimal.ZERO;
        List<DietFoodItemDTO> items = record.getFoodItems();
        if (items == null)
            return BigDecimal.ZERO;
        for (DietFoodItemDTO item : items) {
            sum = sum.add(defaultIfNull(item.getCalories()));
        }
        return sum;
    }

    private static BigDecimal dayTotalFat(DietRecordSimpleVO record) {
        BigDecimal sum = BigDecimal.ZERO;
        List<DietFoodItemDTO> items = record.getFoodItems();
        if (items == null)
            return BigDecimal.ZERO;
        for (DietFoodItemDTO item : items) {
            sum = sum.add(defaultIfNull(item.getFat()));
        }
        return sum;
    }

    private static BigDecimal dayTotalCarb(DietRecordSimpleVO record) {
        BigDecimal sum = BigDecimal.ZERO;
        List<DietFoodItemDTO> items = record.getFoodItems();
        if (items == null)
            return BigDecimal.ZERO;
        for (DietFoodItemDTO item : items) {
            sum = sum.add(defaultIfNull(item.getCarbohydrate()));
        }
        return sum;
    }

    private static int calculateConsecutiveDietRecords(List<DietRecords> records, Predicate<DietRecords> condition) {
        int count = 0;
        for (DietRecords record : records) {
            if (condition.test(record))
                count++;
            else
                break;
        }
        return count;
    }

    private static int calculateConsecutiveSimpleVO(List<DietRecordSimpleVO> records,
            Predicate<DietRecordSimpleVO> condition) {
        int count = 0;
        for (DietRecordSimpleVO record : records) {
            if (condition.test(record))
                count++;
            else
                break;
        }
        return count;
    }

    /**
     * 计算7天平均营养摄入（返回Map键采用 avgCalories/avgProtein/avgFat/avgCarbohydrate）
     */
    public static Map<String, BigDecimal> calculate7DayAverage(List<?> records) {
        if (records == null || records.isEmpty()) {
            return new HashMap<>();
        }
        if (records.get(0) instanceof DietRecords) {
            return calculate7DayAverageFromDietRecords((List<DietRecords>) records);
        } else if (records.get(0) instanceof DietRecordSimpleVO) {
            return calculate7DayAverageFromSimpleVO((List<DietRecordSimpleVO>) records);
        }
        return new HashMap<>();
    }

    private static Map<String, BigDecimal> calculate7DayAverageFromDietRecords(List<DietRecords> records) {
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal totalCalories = BigDecimal.ZERO;
        for (DietRecords r : records) {
            totalCalories = totalCalories.add(r.getTotalCalories() == null ? BigDecimal.ZERO : r.getTotalCalories());
        }
        BigDecimal days = BigDecimal.valueOf(Math.max(1, records.size()));
        result.put("avgCalories", totalCalories.divide(days, 2, BigDecimal.ROUND_HALF_UP));
        return result;
    }

    private static Map<String, BigDecimal> calculate7DayAverageFromSimpleVO(List<DietRecordSimpleVO> records) {
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal totalCal = BigDecimal.ZERO;
        BigDecimal totalPro = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalCarb = BigDecimal.ZERO;
        for (DietRecordSimpleVO r : records) {
            totalCal = totalCal.add(dayTotalCalories(r));
            totalPro = totalPro.add(sumBy(r, DietFoodItemDTO::getProtein));
            totalFat = totalFat.add(sumBy(r, DietFoodItemDTO::getFat));
            totalCarb = totalCarb.add(sumBy(r, DietFoodItemDTO::getCarbohydrate));
        }
        BigDecimal days = BigDecimal.valueOf(Math.max(1, records.size()));
        result.put("avgCalories", totalCal.divide(days, 2, BigDecimal.ROUND_HALF_UP));
        result.put("avgProtein", totalPro.divide(days, 2, BigDecimal.ROUND_HALF_UP));
        result.put("avgFat", totalFat.divide(days, 2, BigDecimal.ROUND_HALF_UP));
        result.put("avgCarbohydrate", totalCarb.divide(days, 2, BigDecimal.ROUND_HALF_UP));
        return result;
    }

    private static BigDecimal sumBy(DietRecordSimpleVO record,
            java.util.function.Function<DietFoodItemDTO, BigDecimal> getter) {
        List<DietFoodItemDTO> items = record.getFoodItems();
        if (items == null)
            return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (DietFoodItemDTO item : items) {
            sum = sum.add(defaultIfNull(getter.apply(item)));
        }
        return sum;
    }

    private static BigDecimal defaultIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}