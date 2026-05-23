package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zr.health.events.DietRecordCreatedEvent;
import com.zr.health.mapper.DietFoodItemsMapper;
import com.zr.health.mapper.DietRecordsMapper;
import com.zr.health.mapper.FoodLibraryMapper;
import com.zr.health.model.dto.food.DietFoodItemDTO;
import com.zr.health.model.dto.food.DietFoodItemUpdateDTO;
import com.zr.health.model.dto.food.DietRecordAddDTO;
import com.zr.health.model.dto.food.DietRecordUpdateDTO;
import com.zr.health.model.entity.DietFoodItems;
import com.zr.health.model.entity.DietRecords;
import com.zr.health.model.entity.FoodLibrary;
import com.zr.health.model.vo.food.DietRecordDetailVO;
import com.zr.health.model.vo.food.DietRecordSimpleVO;
import com.zr.health.service.DietRecordsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 针对表【diet_records(饮食记录主表)】的数据库操作Service实现类
 * 实现饮食记录的增删查等业务逻辑
 */
@Slf4j
@Service
public class DietRecordsServiceImpl extends ServiceImpl<DietRecordsMapper, DietRecords>
        implements DietRecordsService {

    @Resource
    private DietRecordsMapper dietRecordsMapper;

    @Resource
    private DietFoodItemsMapper dietFoodItemsMapper;

    @Resource
    private FoodLibraryMapper foodLibraryMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * 添加一条饮食记录，包括主记录和对应的多个食物项
     * 
     * @param dto 添加饮食记录的数据传输对象
     * @return 添加成功返回true，否则false
     */
    @Override
    public BigDecimal addDietRecord(DietRecordAddDTO dto) {
        BigDecimal totalCalories = BigDecimal.ZERO;

        // 创建主记录
        DietRecords record = new DietRecords();
        record.setUserId(dto.getUserId());
        record.setMealType(dto.getMealType());
        record.setRecordDate(new Date());
        record.setRecordTime(new Date());
        record.setNote(dto.getNote());
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        record.setIsDelete(0);

        int insert = dietRecordsMapper.insert(record);
        if (insert <= 0) {
            return BigDecimal.ZERO; // 插入失败，返回 0 热量
        }
        // 发布记录创建事件，触发健康风险评估（使用Spring注入的发布器）
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(new DietRecordCreatedEvent(dto.getUserId()));
        }

        Long recordId = record.getId();

        // 插入食物项
        if (dto.getFoodItems() != null && !dto.getFoodItems().isEmpty()) {
            List<DietFoodItems> items = new ArrayList<>();
            for (DietFoodItemDTO itemDTO : dto.getFoodItems()) {
                Long foodId = itemDTO.getFoodId();
                BigDecimal amount = itemDTO.getAmount();

                FoodLibrary food = foodLibraryMapper.selectById(foodId);
                if (food != null && amount != null) {
                    DietFoodItems item = new DietFoodItems();
                    item.setRecordId(recordId);
                    item.setFoodId(foodId);
                    item.setFoodName(food.getName());
                    item.setAmount(amount);
                    item.setUnit(itemDTO.getUnit() == null || itemDTO.getUnit().isBlank() ? "g" : itemDTO.getUnit());

                    // 营养素 = 每100g值 * 实际克数 / 100
                    BigDecimal calories = safeMultiply(food.getCalories(), amount).divide(BigDecimal.valueOf(100), 2,
                            BigDecimal.ROUND_HALF_UP);
                    BigDecimal protein = safeMultiply(food.getProtein(), amount).divide(BigDecimal.valueOf(100), 2,
                            BigDecimal.ROUND_HALF_UP);
                    BigDecimal fat = safeMultiply(food.getFat(), amount).divide(BigDecimal.valueOf(100), 2,
                            BigDecimal.ROUND_HALF_UP);
                    BigDecimal carbohydrate = safeMultiply(food.getCarbohydrate(), amount)
                            .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

                    item.setCalories(calories);
                    item.setProtein(protein);
                    item.setFat(fat);
                    item.setCarbohydrate(carbohydrate);

                    item.setImages(food.getImage());
                    item.setNote(itemDTO.getNote());
                    item.setCreateTime(new Date());
                    item.setUpdateTime(new Date());
                    item.setIsDelete(0);
                    items.add(item);

                    // 累加总热量
                    totalCalories = totalCalories.add(calories);

                }
            }

            for (DietFoodItems item : items) {
                dietFoodItemsMapper.insert(item);
            }
        }

        // 新增：更新主记录的总热量
        if (recordId != null && totalCalories.compareTo(BigDecimal.ZERO) > 0) {
            DietRecords updateRecord = new DietRecords();
            updateRecord.setId(recordId);
            updateRecord.setTotalCalories(totalCalories);
            dietRecordsMapper.updateById(updateRecord);
        }

        return totalCalories;
    }

    private BigDecimal safeMultiply(BigDecimal value1, BigDecimal value2) {
        if (value1 == null || value2 == null) {
            return BigDecimal.ZERO;
        }
        return value1.multiply(value2);
    }

    @Override
    public BigDecimal replaceDietRecord(DietRecordAddDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getMealType() == null) {
            return BigDecimal.ZERO;
        }

        LocalDate localDate = LocalDate.now();
        Date startDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date now = new Date();

        QueryWrapper<DietRecords> mealWrapper = new QueryWrapper<>();
        mealWrapper.eq("user_id", dto.getUserId())
                .eq("meal_type", dto.getMealType())
                .ge("record_date", startDate)
                .lt("record_date", endDate)
                .eq("is_delete", 0)
                .orderByDesc("update_time")
                .orderByDesc("create_time")
                .orderByDesc("id");
        List<DietRecords> mealRecords = dietRecordsMapper.selectList(mealWrapper);

        List<DietFoodItemDTO> validFoodItems = dto.getFoodItems() == null ? Collections.emptyList() :
                dto.getFoodItems().stream()
                        .filter(item -> item.getFoodId() != null
                                && item.getAmount() != null
                                && item.getAmount().compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toList());

        if (validFoodItems.isEmpty()) {
            for (DietRecords oldRecord : mealRecords) {
                dietFoodItemsMapper.deleteByRecordId(oldRecord.getId());
                oldRecord.setIsDelete(1);
                oldRecord.setUpdateTime(now);
                dietRecordsMapper.updateById(oldRecord);
            }
            if (this.applicationEventPublisher != null) {
                this.applicationEventPublisher.publishEvent(new DietRecordCreatedEvent(dto.getUserId()));
            }
            return BigDecimal.ZERO;
        }

        DietRecords record = mealRecords.isEmpty() ? null : mealRecords.get(0);
        if (record == null) {
            record = new DietRecords();
            record.setUserId(dto.getUserId());
            record.setMealType(dto.getMealType());
            record.setRecordDate(now);
            record.setRecordTime(now);
            record.setNote(dto.getNote());
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setIsDelete(0);
            int insert = dietRecordsMapper.insert(record);
            if (insert <= 0) {
                return BigDecimal.ZERO;
            }
        } else {
            record.setNote(dto.getNote());
            record.setRecordTime(now);
            record.setUpdateTime(now);
            record.setIsDelete(0);
            dietRecordsMapper.updateById(record);
        }

        dietFoodItemsMapper.deleteByRecordId(record.getId());

        for (int i = 1; i < mealRecords.size(); i++) {
            DietRecords duplicateRecord = mealRecords.get(i);
            dietFoodItemsMapper.deleteByRecordId(duplicateRecord.getId());
            duplicateRecord.setIsDelete(1);
            duplicateRecord.setUpdateTime(now);
            dietRecordsMapper.updateById(duplicateRecord);
        }

        BigDecimal totalCalories = BigDecimal.ZERO;
        List<DietFoodItems> items = new ArrayList<>();
        for (DietFoodItemDTO itemDTO : validFoodItems) {
            Long foodId = itemDTO.getFoodId();
            BigDecimal amount = itemDTO.getAmount();

            FoodLibrary food = foodLibraryMapper.selectById(foodId);
            if (food == null) {
                continue;
            }

            DietFoodItems item = new DietFoodItems();
            item.setRecordId(record.getId());
            item.setFoodId(foodId);
            item.setFoodName(food.getName());
            item.setAmount(amount);
            item.setUnit(itemDTO.getUnit() == null || itemDTO.getUnit().isBlank() ? "g" : itemDTO.getUnit());

            BigDecimal calories = safeMultiply(food.getCalories(), amount)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal protein = safeMultiply(food.getProtein(), amount)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal fat = safeMultiply(food.getFat(), amount)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal carbohydrate = safeMultiply(food.getCarbohydrate(), amount)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

            item.setCalories(calories);
            item.setProtein(protein);
            item.setFat(fat);
            item.setCarbohydrate(carbohydrate);
            item.setImages(food.getImage());
            item.setNote(itemDTO.getNote());
            item.setCreateTime(now);
            item.setUpdateTime(now);
            item.setIsDelete(0);
            items.add(item);
            totalCalories = totalCalories.add(calories);
        }
        if (!items.isEmpty()) {
            dietFoodItemsMapper.batchInsert(items);
        }

        DietRecords updateRecord = new DietRecords();
        updateRecord.setId(record.getId());
        updateRecord.setTotalCalories(totalCalories);
        updateRecord.setRecordTime(now);
        updateRecord.setNote(dto.getNote());
        updateRecord.setUpdateTime(now);
        dietRecordsMapper.updateById(updateRecord);

        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(new DietRecordCreatedEvent(dto.getUserId()));
        }
        return totalCalories;
    }

    /**
     * 查询指定用户某天的饮食记录（简要信息）
     * 
     * @param userId 用户ID
     * @param date   指定日期，null则默认今天
     * @return 饮食记录简要信息列表
     */
    @Override
    public List<DietRecordSimpleVO> getTodayRecords(Long userId, Date date) {
        LocalDate localDate;
        if (date == null) {
            localDate = LocalDate.now();
        } else {
            localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        Date startDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        QueryWrapper<DietRecords> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .ge("record_date", startDate)
                .lt("record_date", endDate)
                .eq("is_delete", 0);

        List<DietRecords> records = dietRecordsMapper.selectList(wrapper);

        List<DietRecordSimpleVO> result = new ArrayList<>();
        for (DietRecords record : records) {
            DietRecordSimpleVO vo = new DietRecordSimpleVO();
            BeanUtils.copyProperties(record, vo);

            List<DietFoodItems> foodItems = dietFoodItemsMapper.selectByRecordId(record.getId());
            List<DietFoodItemDTO> foodDTOs = foodItems.stream().map(item -> {
                DietFoodItemDTO dto = new DietFoodItemDTO();
                BeanUtils.copyProperties(item, dto);

                // 查询食物库获取名称和图片（与getHistoryRecords保持一致）
                if (item.getFoodId() != null) {
                    FoodLibrary food = foodLibraryMapper.selectById(item.getFoodId());
                    if (food != null) {
                        dto.setFoodName(food.getName());
                        dto.setFoodImage(food.getImage());
                        dto.setCategoryName(food.getCategoryName());
                    }
                } else {
                    dto.setFoodName(item.getFoodName());
                    dto.setFoodImage(null);
                    dto.setCategoryName(null);
                }

                return dto;
            }).collect(Collectors.toList());
            vo.setFoodItems(foodDTOs);
            result.add(vo);
        }
        return result;
    }

    /**
     * 查询指定用户最近N天的饮食历史记录（简要信息）
     * 
     * @param userId 用户ID
     * @param days   天数，若为null则查询全部
     * @return 饮食记录简要信息列表
     */
    @Override
    public List<DietRecordSimpleVO> getHistoryRecords(Long userId, Integer days) {
        QueryWrapper<DietRecords> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_delete", 0)
                .orderByDesc("record_date")
                .orderByDesc("create_time");

        // 若指定天数，筛选最近days天内的记录
        if (days != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -days);
            wrapper.ge("record_date", cal.getTime());
        }

        List<DietRecords> records = dietRecordsMapper.selectList(wrapper);
        List<DietRecordSimpleVO> result = new ArrayList<>();

        for (DietRecords record : records) {
            DietRecordSimpleVO vo = new DietRecordSimpleVO();
            BeanUtils.copyProperties(record, vo);

            List<DietFoodItems> foodItems = dietFoodItemsMapper.selectByRecordId(record.getId());
            List<DietFoodItemDTO> foodDTOs = foodItems.stream().map(item -> {
                DietFoodItemDTO dto = new DietFoodItemDTO();
                BeanUtils.copyProperties(item, dto);

                // 关键修改：查询食物库获取名称和图片
                if (item.getFoodId() != null) {
                    FoodLibrary food = foodLibraryMapper.selectById(item.getFoodId());
                    if (food != null) {
                        dto.setFoodName(food.getName());
                        dto.setFoodImage(food.getImage());
                        dto.setCategoryName(food.getCategoryName());
                    }
                } else {
                    dto.setFoodName(item.getFoodName());
                    dto.setFoodImage(null);
                    dto.setCategoryName(null);
                }

                return dto;
            }).collect(Collectors.toList());

            vo.setFoodItems(foodDTOs);
            result.add(vo);
        }
        return result;
    }

    /**
     * 根据饮食记录ID查询详细信息，包括食物项列表
     * 
     * @param id 饮食记录ID
     * @return 饮食记录详情VO，若记录不存在或已删除返回null
     */
    @Override
    public DietRecordDetailVO getDietRecordDetail(Long id) {
        DietRecords record = dietRecordsMapper.selectById(id);
        if (record == null || record.getIsDelete() == 1)
            return null;

        DietRecordDetailVO vo = new DietRecordDetailVO();
        BeanUtils.copyProperties(record, vo);

        List<DietFoodItems> foodItems = dietFoodItemsMapper.selectByRecordId(id);
        List<DietFoodItemDTO> foodDTOs = foodItems.stream().map(item -> {
            DietFoodItemDTO dto = new DietFoodItemDTO();
            BeanUtils.copyProperties(item, dto);
            return dto;
        }).collect(Collectors.toList());
        vo.setFoodItems(foodDTOs);
        return vo;
    }

    /**
     * 软删除饮食记录（设置isDelete为1），同时软删除对应的食物项
     * 
     * @param id 饮食记录ID
     * @return 删除成功返回true，否则false
     */
    @Override
    public boolean deleteDietRecord(Long id) {
        DietRecords record = dietRecordsMapper.selectById(id);
        if (record == null)
            return false;

        record.setIsDelete(1);
        record.setUpdateTime(new Date());

        int updateMain = dietRecordsMapper.updateById(record);
        int updateDetail = dietFoodItemsMapper.softDeleteByRecordId(id);

        // 主记录和食物详情都需成功删除才返回true
        return updateMain > 0 && updateDetail > 0;
    }

    @Override
    public boolean updateDietRecord(DietRecordUpdateDTO updateDTO) {
        if (updateDTO == null || updateDTO.getId() == null) {
            return false;
        }

        DietRecords record = dietRecordsMapper.selectById(updateDTO.getId());
        if (record == null || record.getIsDelete() == 1) {
            return false;
        }

        // 更新主表信息
        record.setRecordDate(updateDTO.getRecordDate());
        record.setMealType(updateDTO.getMealType());
        record.setNote(updateDTO.getNote());
        record.setUpdateTime(new Date());

        int updateCount = dietRecordsMapper.updateById(record);
        if (updateCount <= 0) {
            return false;
        }

        // 先删除旧食物项
        dietFoodItemsMapper.deleteByRecordId(record.getId());

        // 插入新食物项
        if (updateDTO.getFoodItems() != null && !updateDTO.getFoodItems().isEmpty()) {
            List<DietFoodItems> newFoodItems = new ArrayList<>();
            for (DietFoodItemUpdateDTO itemDTO : updateDTO.getFoodItems()) {
                FoodLibrary food = foodLibraryMapper.selectById(itemDTO.getFoodId());
                if (food == null) {
                    // 没查到对应食物，可以跳过或者抛异常
                    continue;
                }

                DietFoodItems item = new DietFoodItems();
                item.setRecordId(record.getId());
                item.setFoodId(itemDTO.getFoodId());
                item.setFoodName(food.getName()); // 食物名称

                item.setAmount(itemDTO.getAmount());
                item.setUnit(itemDTO.getUnit());
                item.setNote(itemDTO.getNote());

                // 计算营养成分，防止空指针，参考你的safeMultiply方法
                BigDecimal amount = itemDTO.getAmount() == null ? BigDecimal.ZERO : itemDTO.getAmount();
                BigDecimal hundred = BigDecimal.valueOf(100);

                item.setCalories(safeDivide(safeMultiply(food.getCalories(), amount), hundred));
                item.setProtein(safeDivide(safeMultiply(food.getProtein(), amount), hundred));
                item.setFat(safeDivide(safeMultiply(food.getFat(), amount), hundred));
                item.setCarbohydrate(safeDivide(safeMultiply(food.getCarbohydrate(), amount), hundred));

                item.setImages(food.getImage());

                item.setCreateTime(new Date());
                item.setUpdateTime(new Date());
                item.setIsDelete(0);

                newFoodItems.add(item);
            }
            dietFoodItemsMapper.batchInsert(newFoodItems);
        }

        return true;
    }

    /**
     * 获取用户当天摄入的总热量（直接从diet_records表获取）
     *
     * @param userId 用户ID
     * @return 总热量，如果没有记录则返回0
     */
    public BigDecimal getTodayTotalCalories(Long userId) {
        LocalDate today = LocalDate.now();
        Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 查询当天的所有饮食记录
        List<DietRecords> records = dietRecordsMapper.selectByUserIdAndDate(userId, todayDate);

        // 添加日志验证查询结果
        log.info("用户 {} 当天饮食记录数量: {}", userId, records.size());
        records.forEach(record -> {
            log.info("记录ID: {}, 总热量: {}", record.getId(), record.getTotalCalories());
        });

        return records.stream()
                .map(DietRecords::getTotalCalories)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 除法安全计算，避免除以零或null
    private BigDecimal safeDivide(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || BigDecimal.ZERO.compareTo(b) == 0) {
            return BigDecimal.ZERO;
        }
        return a.divide(b, 2, BigDecimal.ROUND_HALF_UP);
    }

}
