package com.zr.health.utils;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 表达式工具类（使用 Spring SpEL 实现）
 */
public class MvelUtil {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private static StandardEvaluationContext buildContext(Map<?, ?> map) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.addPropertyAccessor(new MapAccessor());
        Map<?, ?> safe = (map == null) ? Collections.emptyMap() : map;
        context.setRootObject(safe);
        safe.forEach((k, v) -> context.setVariable(String.valueOf(k), v));
        return context;
    }

    public static BigDecimal execute(String formula, Map<String, BigDecimal> map) {
        try {
            StandardEvaluationContext context = buildContext(map);
            Expression expr = PARSER.parseExpression(formula);
            Object result = expr.getValue(context);
            if (result instanceof Number) {
                return new BigDecimal(result.toString());
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            throw new RuntimeException("表达式执行失败: " + formula, e);
        }
    }

    public static boolean executeBoolean(String expression, Map<String, Object> map) {
        try {
            StandardEvaluationContext context = buildContext(map);
            Expression expr = PARSER.parseExpression(expression);
            Object result = expr.getValue(context);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            throw new RuntimeException("布尔表达式执行失败: " + expression, e);
        }
    }

    public static boolean isValidExpression(String expression) {
        try {
            PARSER.parseExpression(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidBooleanExpression(String expression) {
        try {
            PARSER.parseExpression(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}