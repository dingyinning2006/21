package com.example.demo.skill;

import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计算器 Skill：关键词命中后提取数学表达式并计算
 * 不经过 LLM，确定性执行
 */
@Component
public class CalculatorSkill implements Skill {

    // 触发关键词
    private static final List<String> KEYWORDS = Arrays.asList(
            "计算", "算一下", "等于多少", "是多少",
            "加", "减", "乘", "除", "乘以", "除以",
            "计算器", "算一算"
    );

    // 匹配数学表达式的正则：数字、运算符、括号、小数点
    private static final Pattern EXPRESSION_PATTERN =
            Pattern.compile("[\\d.+\\-*/()]+");

    @Override
    public List<String> getKeywords() {
        return KEYWORDS;
    }

    @Override
    public String execute(String userMessage) {
        // 1. 从用户消息中提取数学表达式
        String expression = extractExpression(userMessage);

        if (expression == null || expression.isBlank()) {
            return "请输入要计算的表达式，例如：计算 2+3*4、(10-2)/3";
        }

        // 2. 计算
        try {
            double result = evaluate(expression);
            // 整数就不显示小数点
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return expression + " = " + (long) result;
            }
            return expression + " = " + result;
        } catch (Exception e) {
            return "计算失败：" + e.getMessage() + "。请检查表达式是否正确。";
        }
    }

    /**
     * 从文本中提取最长的数学表达式
     */
    private String extractExpression(String text) {
        if (text == null) return null;
        Matcher matcher = EXPRESSION_PATTERN.matcher(text);
        String longest = null;
        while (matcher.find()) {
            String match = matcher.group().trim();
            // 至少包含一个运算符才认为是表达式（排除纯数字）
            if (match.matches(".*[+\\-*/].*") && match.length() > 1) {
                if (longest == null || match.length() > longest.length()) {
                    longest = match;
                }
            }
        }
        return longest;
    }

    /**
     * 执行计算
     */
    private double evaluate(String expression) throws Exception {
        // 安全检查：只允许数字、运算符、括号、小数点
        if (!expression.matches("[\\d.+\\-*/()\\s]+")) {
            throw new IllegalArgumentException("表达式包含非法字符");
        }
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        return ((Number) engine.eval(expression)).doubleValue();
    }
}
