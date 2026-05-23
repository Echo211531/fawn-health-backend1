package com.zr.health.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.mapper.*;
import com.zr.health.model.dto.food.DietRecordDTO;
import com.zr.health.model.dto.food.NutritionAnalysisResult;
import com.zr.health.model.dto.food.NutritionData;
import com.zr.health.model.entity.*;
import com.zr.health.service.HealthAssessmentService;
import com.zr.health.service.HealthReportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
* @author 27105
* @description 针对表【health_assessment(用户健康评估每日记录表)】的数据库操作Service实现
* @createDate 2025-08-09 20:15:30
*/
@Slf4j
@Service
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment>
        implements HealthAssessmentService {

    // 常量定义
    public static final int DIET_ADVICE = 1;
    public static final int EXERCISE_ADVICE = 2;

    @Resource
    private HealthAssessmentMapper healthAssessmentMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserWeightHistoryMapper userWeightHistoryMapper;

    @Resource
    private AdviceRulesMapper adviceRulesMapper;

    @Resource
    private HealthReportService healthReportService;

    @Resource
    private DietRecordsMapper dietRecordsMapper;

    @Resource
    private DietFoodItemsMapper dietFoodItemsMapper;

    // 线程池配置 - 根据服务器性能调整
    private final ExecutorService assessmentExecutor = new ThreadPoolExecutor(
            5, // 核心线程数
            10, // 最大线程数
            60, TimeUnit.SECONDS, // 空闲线程存活时间
            new LinkedBlockingQueue<>(100), // 任务队列
            new ThreadPoolExecutor.CallerRunsPolicy() // 任务拒绝策略（超出队列时由提交线程执行）
    );

    /**
     * 执行每日健康评估
     */
    // ┌──────────┬──────┬──────┬─────────────────────────────────────────┐
    //  │   维度   │ 满分 │ 权重 │              关键数据来源               │
    //  ├──────────┼──────┼──────┼─────────────────────────────────────────┤
    //  │ 体重趋势 │ 50   │ 40%  │ 最近7天体重记录 + 目标体重              │
    //  ├──────────┼──────┼──────┼─────────────────────────────────────────┤
    //  │   维度   │ 满分 │ 权重 │              关键数据来源               │
    //  ├──────────┼──────┼──────┼─────────────────────────────────────────┤
    //  │ 体重趋势 │ 50   │ 40%  │ 最近7天体重记录 + 目标体重              │
    //  ├──────────┼──────┼──────┼─────────────────────────────────────────┤
    //  │ BMI      │ 50   │ 40%  │ 身高 + 当前体重                         │
    //  ├──────────┼──────┼──────┼─────────────────────────────────────────┤
    //  │ 营养均衡 │ 20   │ 20%  │ 最近7天饮食记录（蛋白质/脂肪/碳水比例） │
    //  └──────────┴──────┴──────┴─────────────────────────────────────────┘
    @Transactional
    @Override
    public HealthAssessment dailyAssessment(Long userId) {
        // 1. 生成"纯日期"（无时间部分，确保与数据库Date类型匹配）
        // 获取当前日期的起始时间（当天00:00:00）
        LocalDate today = LocalDate.now();
        ZonedDateTime zonedDateTime = today.atStartOfDay(ZoneId.systemDefault());
        Date assessmentDate = Date.from(zonedDateTime.toInstant()); // 仅包含年月日的Date对象

        // 2. 检查是否已评估（使用纯日期检查）
        // 替换原有的exists检查
        if (healthAssessmentMapper.existsWithLock(userId, assessmentDate)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已评估");
        }
        // 3. 获取用户基础信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 4. 获取最近7天数据（日期范围调整为纯日期）
        // 修正：获取最近7天的完整日期范围（包含当天）
        LocalDate todayLocal = LocalDate.now();
        LocalDate startLocal = todayLocal.minusDays(6); // 前6天（共7天）

        // 转换为包含时间的Date：开始时间为startLocal的0点，结束时间为todayLocal的23:59:59
        Date startDate = Date.from(startLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(todayLocal.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        // 查询体重记录（使用修正后的日期范围）
        List<UserWeightHistory> weights = userWeightHistoryMapper.selectByUserIdAndDateRange(
                userId, startDate, endDate);

        // 饮食数据
        List<DietRecordDTO> dietRecords = getDietRecords(userId, startDate, endDate);

        // 5. 计算各项指标（不变）
        BigDecimal weightTrend = calculateWeightTrend(weights);
        NutritionAnalysisResult nutritionResult = analyzeNutrition(dietRecords, user);
        Integer score = calculateCompositeScore(user, weightTrend, weights, nutritionResult);

        // 6. 生成建议（不变）
        List<AdviceRules> rules = adviceRulesMapper.selectAllOrderByPriority();
        String dietAdvice = generateAdvice(rules, score, weightTrend, nutritionResult, DIET_ADVICE, user);
        String exerciseAdvice = generateAdvice(rules, score, weightTrend, nutritionResult, EXERCISE_ADVICE, user);

        // 7. 保存评估结果（使用纯日期存储）
        HealthAssessment assessment = new HealthAssessment();
        assessment.setUserId(userId);
        assessment.setScore(score);
        assessment.setWeightTrend(weightTrend);
        assessment.setCalorieBalance(nutritionResult.getCalorieBalance());
        assessment.setNutritionScore(nutritionResult.getNutritionScore());
        assessment.setDietAdvice(dietAdvice);
        assessment.setExerciseAdvice(exerciseAdvice);
        assessment.setAssessmentDate(assessmentDate); // 关键：存储纯日期

        healthAssessmentMapper.insert(assessment);

        return assessment;
    }

    private List<DietRecordDTO> getDietRecords(Long userId, Date startDate, Date endDate) {
        List<DietRecords> records = dietRecordsMapper.selectByUserIdAndDateRange(userId, startDate, endDate);

        return records.stream().map(record -> {
            List<DietFoodItems> items = dietFoodItemsMapper.selectByRecordId(record.getId());

            DietRecordDTO dto = new DietRecordDTO();
            dto.setRecordDate(record.getRecordDate());
            dto.setMealType(record.getMealType());

            // 计算单条记录的营养数据 - 处理可能的null值
            NutritionData nutrition = items.stream()
                    .map(item -> new NutritionData(
                            item.getCalories() != null ? item.getCalories() : BigDecimal.ZERO,
                            item.getProtein() != null ? item.getProtein() : BigDecimal.ZERO,
                            item.getFat() != null ? item.getFat() : BigDecimal.ZERO,
                            item.getCarbohydrate() != null ? item.getCarbohydrate() : BigDecimal.ZERO
                    ))
                    .reduce(new NutritionData(), NutritionData::add);

            dto.setNutrition(nutrition);
            return dto;
        }).collect(Collectors.toList());
    }

    // 营养分析
    private NutritionAnalysisResult analyzeNutrition(List<DietRecordDTO> dietRecords, User user) {
        NutritionAnalysisResult result = new NutritionAnalysisResult();

        if (dietRecords == null || dietRecords.isEmpty()) {
            log.warn("未获取到饮食记录，返回默认营养分析结果");
            result.setNutritionScore(50); // 中等默认分
            result.setCalorieBalance(BigDecimal.ZERO);
            return result;
        }

        // 计算平均每日营养摄入（完善空值处理）
        NutritionData totalNutrition = dietRecords.stream()
                .map(DietRecordDTO::getNutrition)
                .filter(Objects::nonNull) // 过滤空营养数据
                .reduce(new NutritionData(), NutritionData::add);

        long distinctDays = dietRecords.stream()
                .map(DietRecordDTO::getRecordDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // 处理天数为0的情况
        NutritionData averageDaily = distinctDays > 0 ?
                totalNutrition.divide(distinctDays) : totalNutrition;

        result.setAverageDailyNutrition(averageDaily);

        // 计算热量平衡（完善用户每日热量需求的空值处理）
        if (user.getDailyCalories() != null) {
            BigDecimal balance = averageDaily.getCalories()
                    .subtract(user.getDailyCalories());
            result.setCalorieBalance(balance);
        } else {
            result.setCalorieBalance(BigDecimal.ZERO);
            log.warn("用户[ID: {}]未设置每日热量需求，热量平衡默认设为0", user.getId());
        }

        // 计算营养均衡评分
        result.setNutritionScore(calculateNutritionScore(averageDaily));

        return result;
    }

    // 计算营养均衡评分
    private Integer calculateNutritionScore(NutritionData nutrition) {
        // 理想比例: 蛋白质15%-25%, 脂肪20%-30%, 碳水50%-60%
        BigDecimal total = nutrition.getProtein()
                .add(nutrition.getFat())
                .add(nutrition.getCarbohydrate());

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        BigDecimal proteinRatio = nutrition.getProtein().divide(total, 2, RoundingMode.HALF_UP);
        BigDecimal fatRatio = nutrition.getFat().divide(total, 2, RoundingMode.HALF_UP);

        int score = 100;

        // 蛋白质比例检查
        if (proteinRatio.compareTo(new BigDecimal("0.15")) < 0) {
            score -= 20;
        } else if (proteinRatio.compareTo(new BigDecimal("0.25")) > 0) {
            score -= 10;
        }

        // 脂肪比例检查
        if (fatRatio.compareTo(new BigDecimal("0.20")) < 0) {
            score -= 15;
        } else if (fatRatio.compareTo(new BigDecimal("0.30")) > 0) {
            score -= 15;
        }

        return Math.max(0, score);
    }

    /**
     * 获取用户最新健康评估结果
     */
    @Override
    public HealthAssessment getLatestByUser(Long userId) {
        return lambdaQuery()
                .eq(HealthAssessment::getUserId, userId)
                .orderByDesc(HealthAssessment::getAssessmentDate)
                .last("LIMIT 1")
                .one();
    }

    // 计算体重趋势(kg/周)
    private BigDecimal calculateWeightTrend(List<UserWeightHistory> records) {
        // 1. 校验输入数据
        if (records == null || records.size() < 2) {
            // 数据不足时返回null或默认值
            return BigDecimal.ZERO;
        }

        SimpleRegression regression = new SimpleRegression();

        // 2. 过滤并添加有效数据
        for (UserWeightHistory record : records) {
            // 校验记录对象和日期不为null
            if (record == null || record.getRecordDate() == null) {
                continue;
            }

            // 校验体重值有效
            BigDecimal weight = record.getWeight();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 跳过无效体重值
            }

            // 转换日期为天数（降低时间精度，减少计算误差）
            long days = record.getRecordDate().getTime() / (24 * 60 * 60 * 1000);
            regression.addData(days, weight.doubleValue());
        }

        // 3. 校验回归分析所需数据量
        if (regression.getN() < 2) {
            // 有效数据不足时返回默认值
            return BigDecimal.ZERO;
        }

        // 4. 计算周变化趋势
        double slope = regression.getSlope();
        if (Double.isNaN(slope) || Double.isInfinite(slope)) {
            return BigDecimal.ZERO;
        }

        // 斜率 * 7天（因为X轴已转换为天数）
        double weeklyTrend = slope * 7;

        // 5. 格式化结果
        return BigDecimal.valueOf(weeklyTrend)
                .setScale(2, RoundingMode.HALF_UP);
    }


    // 计算综合评分(0-100)
    private Integer calculateCompositeScore(User user, BigDecimal weightTrend,
                                            List<UserWeightHistory> weights,
                                            NutritionAnalysisResult nutritionResult) {
        // 1. 计算各项得分（增加范围限制）
        double trendScore = Math.max(0, Math.min(50, calculateTrendScore(user, weightTrend, weights)));
        double bmiScore = Math.max(0, Math.min(50, calculateBmiScore(user)));
        double nutritionScore = Math.max(0, Math.min(20, calculateNutritionImpactScore(nutritionResult)));

        // 2. 综合评分计算（确保总分在0-100之间）
        int totalScore = (int) Math.round(trendScore * 0.4 + bmiScore * 0.4 + nutritionScore * 0.2);
        return Math.max(0, Math.min(100, totalScore));
    }

    // 新增方法：计算营养对评分的影响(0-20分)
    private double calculateNutritionImpactScore(NutritionAnalysisResult nutritionResult) {
        if (nutritionResult == null || nutritionResult.getNutritionScore() == null) {
            return 10; // 默认中等分数
        }

        // 将营养评分(0-100)转换为0-20分
        return nutritionResult.getNutritionScore() * 0.2;
    }

    // 计算体重变化得分
    private double calculateTrendScore(User user, BigDecimal weightTrend,
                                       List<UserWeightHistory> weights) {
        // 打印查询到的原始记录数
        log.info("用户[ID: {}]查询到的体重记录数: {}", user.getId(), weights == null ? 0 : weights.size());

        // 1. 校验目标体重是否存在
        if (user.getTargetWeight() == null) {
            log.warn("用户[ID: {}]未设置目标体重，返回默认趋势得分", user.getId());
            return 30;
        }

        // 2. 校验体重历史记录是否有效
        if (weights == null || weights.isEmpty()) {
            log.warn("用户[ID: {}]体重记录为空", user.getId());
            return 30;
        }

        // 3. 过滤无效的体重记录，并打印过滤情况
        List<UserWeightHistory> validWeights = weights.stream()
                .filter(record -> {
                    // 详细日志：记录被过滤的原因
                    if (record == null) {
                        log.debug("用户[ID: {}]存在空的体重记录", user.getId());
                        return false;
                    }
                    if (record.getWeight() == null) {
                        log.debug("用户[ID: {}]的体重记录[日期: {}]体重值为null",
                                user.getId(), record.getRecordDate());
                        return false;
                    }
                    if (record.getRecordDate() == null) {
                        log.debug("用户[ID: {}]的体重记录[体重: {}]日期为null",
                                user.getId(), record.getWeight());
                        return false;
                    }
                    return true;
                })
                .toList();

        log.info("用户[ID: {}]过滤后的有效体重记录数: {}", user.getId(), validWeights.size());

        if (validWeights.isEmpty()) {
            log.warn("用户[ID: {}]无有效体重记录（原始记录数: {}）",
                    user.getId(), weights.size());
            return 30;
        }

        // 4. 获取最新的有效体重记录（安全访问）
        UserWeightHistory latestWeightRecord = validWeights.get(validWeights.size() - 1);
        double currentWeight = latestWeightRecord.getWeight().doubleValue();
        double targetDiff = currentWeight - user.getTargetWeight().doubleValue();
        double trend = weightTrend != null ? weightTrend.doubleValue() : 0; // 额外校验weightTrend

        // 5. 根据趋势和目标差异计算得分
        // 如果体重正在向目标靠近
        if ((targetDiff > 0 && trend < 0) || (targetDiff < 0 && trend > 0)) {
            return 50; // 满分
        }
        // 如果体重正在远离目标
        else if ((targetDiff > 0 && trend > 0) || (targetDiff < 0 && trend < 0)) {
            return 10; // 低分
        }
        return 30; // 中性分数（趋势平稳或无明显变化）
    }

    // 计算BMI得分
    private double calculateBmiScore(User user) {
        if (user.getHeight() == null || user.getWeight() == null) {
            return 25; // 默认中等偏下分数
        }

        double height = user.getHeight().doubleValue() / 100; // 转换为米
        double weight = user.getWeight().doubleValue();
        double bmi = weight / (height * height);

        if (bmi >= 18.5 && bmi <= 24) {
            return 50; // 正常范围满分
        } else if (bmi < 18.5 || (bmi > 24 && bmi <= 28)) {
            return 30; // 偏瘦或超重
        } else {
            return 10; // 肥胖或严重偏瘦
        }
    }

    // 计算BMI值
    private BigDecimal calculateBmi(User user) {
        if (user.getHeight() == null || user.getWeight() == null) {
            log.warn("用户[ID: {}]身高或体重为空，无法计算BMI", user.getId());
            return null;
        }

        // 处理身高为0的情况
        if (user.getHeight().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户[ID: {}]身高无效（{}cm），无法计算BMI", user.getId(), user.getHeight());
            return null;
        }

        BigDecimal heightM = user.getHeight().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightM.multiply(heightM);

        // 避免除以零
        if (heightSquared.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("用户[ID: {}]身高平方计算异常，无法计算BMI", user.getId());
            return null;
        }

        return user.getWeight().divide(heightSquared, 1, RoundingMode.HALF_UP);
    }

    private String generateAdvice(List<AdviceRules> rules, Integer score,
                                  BigDecimal weightTrend,
                                  NutritionAnalysisResult nutritionResult,
                                  int adviceType, User user) {
        // 1. 初始化脚本引擎并校验可用性
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript"); // 使用标准名称"JavaScript"而非"js"

        // 校验引擎是否初始化成功
        if (engine == null) {
            log.error("无法获取JavaScript脚本引擎，使用默认建议");
            return getDefaultAdvice(adviceType);
        }

        List<String> matchedAdvice = new ArrayList<>();

        try {
            // 2. 向引擎中放入变量（完善空值处理）
            engine.put("score", score != null ? score : 0);
            engine.put("weightTrend", weightTrend != null ? weightTrend.doubleValue() : 0);
            engine.put("currentWeight", user.getWeight() != null ? user.getWeight().doubleValue() : 0);
            engine.put("targetWeight", user.getTargetWeight() != null ? user.getTargetWeight().doubleValue() : 0);

            BigDecimal bmiValue = calculateBmi(user);
            engine.put("bmi", bmiValue != null ? bmiValue.doubleValue() : 0);

            // 营养相关指标
            if (nutritionResult != null) {
                engine.put("calorieBalance", nutritionResult.getCalorieBalance() != null ?
                        nutritionResult.getCalorieBalance().doubleValue() : 0);
                engine.put("nutritionScore", nutritionResult.getNutritionScore() != null ?
                        nutritionResult.getNutritionScore() : 0);

                if (nutritionResult.getAverageDailyNutrition() != null) {
                    engine.put("protein", nutritionResult.getAverageDailyNutrition().getProtein() != null ?
                            nutritionResult.getAverageDailyNutrition().getProtein().doubleValue() : 0);
                    engine.put("fat", nutritionResult.getAverageDailyNutrition().getFat() != null ?
                            nutritionResult.getAverageDailyNutrition().getFat().doubleValue() : 0);
                    engine.put("carbs", nutritionResult.getAverageDailyNutrition().getCarbohydrate() != null ?
                            nutritionResult.getAverageDailyNutrition().getCarbohydrate().doubleValue() : 0);
                } else {
                    engine.put("protein", 0);
                    engine.put("fat", 0);
                    engine.put("carbs", 0);
                }
            } else {
                engine.put("calorieBalance", 0);
                engine.put("nutritionScore", 0);
                engine.put("protein", 0);
                engine.put("fat", 0);
                engine.put("carbs", 0);
            }

            // 3. 评估规则（过滤无效规则）
            if (rules == null || rules.isEmpty()) {
                log.warn("未找到建议规则，使用默认建议");
                return getDefaultAdvice(adviceType);
            }

            for (AdviceRules rule : rules) {
                // 跳过空条件或空建议的规则
                if (rule.getAdviceType() != adviceType ||
                        rule.getConditionExpr() == null || rule.getConditionExpr().trim().isEmpty() ||
                        rule.getAdviceText() == null || rule.getAdviceText().trim().isEmpty()) {
                    continue;
                }

                try {
                    // 执行规则条件表达式
                    Object evalResult = engine.eval(rule.getConditionExpr());
                    // 严格判断结果为Boolean类型且为true
                    if (evalResult instanceof Boolean && (Boolean) evalResult) {
                        matchedAdvice.add(rule.getAdviceText());
                    }
                } catch (ScriptException e) {
                    log.error("规则[ID: {}]条件执行失败: {}", rule.getId(), rule.getConditionExpr(), e);
                }
            }
        } catch (Exception e) {
            log.error("建议生成异常", e);
            return getDefaultAdvice(adviceType); // 任何异常都返回默认建议
        }

        // 4. 返回匹配的建议或默认建议
        return matchedAdvice.isEmpty() ? getDefaultAdvice(adviceType) : String.join("；", matchedAdvice);
    }

    // 添加默认建议方法
    private String getDefaultAdvice(int adviceType) {
        if (adviceType == DIET_ADVICE) {
            return "建议保持均衡饮食，多摄入蔬菜水果，控制油盐糖摄入。";
        } else {
            return "建议每周进行至少150分钟中等强度有氧运动，如快走、慢跑或游泳。";
        }
    }

    // 每天凌晨2点执行评估
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyAssessmentJob() {
        log.info("开始执行每日健康评估任务...");

        // 获取所有活跃用户
        List<Long> userIds = userMapper.selectActiveUserIds();
        log.info("待评估用户数: {}", userIds.size());

        userIds.forEach(userId -> {
            try {
                dailyAssessment(userId);
                log.debug("用户{}评估完成", userId);
            } catch (Exception e) {
                log.error("用户{}评估失败: {}", userId, e.getMessage());
            }
        });

        log.info("每日健康评估任务完成");
    }

    // 每周一凌晨3点生成周报
    @Scheduled(cron = "0 0 3 ? * MON")
    public void weeklyReportJob() {
        log.info("开始执行健康周报生成任务...");

        List<Long> userIds = userMapper.selectActiveUserIds();
        log.info("待生成周报用户数: {}", userIds.size());

        userIds.forEach(userId -> {
            try {
                healthReportService.generateWeeklyReport(userId);
                log.debug("用户{}周报生成完成", userId);
            } catch (Exception e) {
                log.error("用户{}周报生成失败: {}", userId, e.getMessage());
            }
        });

        log.info("健康周报生成任务完成");
    }

    /**
     * 每天凌晨12点触发的多线程评估任务
     * cron表达式：秒 分 时 日 月 周 → 0 0 0 * * ? 表示每天00:00:00执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void multiThreadDailyAssessmentJob() {
        log.info("===== 开始多线程每日健康评估任务 =====");

        // 1. 获取所有需要评估的活跃用户ID
        List<Long> userIds = userMapper.selectActiveUserIds();
        log.info("待评估用户总数: {}", userIds.size());
        if (userIds.isEmpty()) {
            log.info("无待评估用户，任务结束");
            return;
        }

        // 2. 多线程执行评估任务
        // 将用户ID列表转换为Callable任务列表
        List<Callable<Void>> tasks = userIds.stream()
                .map(userId -> (Callable<Void>) () -> {
                    try {
                        // 调用现有评估方法
                        dailyAssessment(userId);
                        log.debug("用户[{}]评估完成", userId);
                    } catch (BusinessException e) {
                        // 已知异常（如"今日已评估"）不打印堆栈
                        log.warn("用户[{}]评估跳过: {}", userId, e.getMessage());
                    } catch (Exception e) {
                        // 未知异常打印详细日志
                        log.error("用户[{}]评估失败", userId, e);
                    }
                    return null;
                })
                .collect(Collectors.toList());

        try {
            // 3. 执行所有任务并等待完成（超时时间设置为2小时）
            List<Future<Void>> futures = assessmentExecutor.invokeAll(tasks, 2, TimeUnit.HOURS);

            // 4. 统计执行结果
            long successCount = futures.stream()
                    .filter(future -> {
                        try {
                            return !future.isCancelled() && future.get() == null;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            log.info("多线程评估任务完成 - 总用户数: {}, 成功数: {}, 失败/跳过数: {}",
                    userIds.size(), successCount, userIds.size() - successCount);
        } catch (InterruptedException e) {
            log.error("评估任务被中断", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
        } finally {
            log.info("===== 多线程每日健康评估任务结束 =====");
        }
    }
}


