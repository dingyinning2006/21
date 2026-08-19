package com.example.demo.tool;

import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;

@Component
public class CalculatorTool implements Tool {

    @Override
    public String getName() { return "calculator"; }

    @Override
    public String getDescription() {
        return "执行数学计算，支持加(+)、减(-)、乘(*)、除(/)、括号、幂运算(**)。当用户需要计算数学表达式时调用此工具。";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> expression = new LinkedHashMap<>();
        expression.put("type", "string");
        expression.put("description", "数学表达式，例如：'2 + 3 * 4'、'(10 - 2) / 3'");
        expression.put("minLength", 1);
        properties.put("expression", expression);
        return properties;
    }

    @Override
    public List<String> getRequired() {
        return Collections.singletonList("expression");
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String expression = (String) arguments.getOrDefault("expression", "");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expression", expression);
        try {
            double value = evaluate(expression);
            if (value == Math.floor(value) && !Double.isInfinite(value)) {
                result.put("result", (long) value);
            } else {
                result.put("result", value);
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "计算失败：" + e.getMessage());
        }
        return toJson(result);
    }

    private double evaluate(String expression) throws Exception {
        if (!expression.matches("[\\d+\\-*/().\\sMathpowsqrt]+")) {
            throw new IllegalArgumentException("表达式包含非法字符");
        }
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        return ((Number) engine.eval(expression)).doubleValue();
    }

    private String toJson(Map<String, Object> map) {
        // 用你项目里的 JSON 工具类（Jackson/FastJSON）序列化即可
        // 这里简单手写，实际项目建议用 ObjectMapper
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String) sb.append("\"").append(v).append("\"");
            else sb.append(v);
        }
        return sb.append("}").toString();
    }
}