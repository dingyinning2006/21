package com.example.demo.llm;

import com.example.demo.tool.HealthToolService;
import com.example.demo.tool.UnitConverterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.example.demo.skill.Skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * 工具执行器。
 *
 * 模型只会返回工具名称和 JSON 参数，真正的参数校验和 Java 业务调用由这里完成。
 */
@Component
public class ToolExecutor {

    private final UnitConverterService unitConverterService;
    private final HealthToolService healthToolService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Skill> skills;
    public ToolExecutor(
            UnitConverterService unitConverterService,
            HealthToolService healthToolService,
            List<Skill> skills
    ) {
        this.unitConverterService = unitConverterService;
        this.healthToolService = healthToolService;

        this.skills = new HashMap<>();
        for (Skill skill : skills) {
            this.skills.put(skill.getName(), skill);
        }
    }

    /**
     * 根据工具名称分发执行，并把结果统一转换成字符串交还给模型。
     */
    public String execute(String toolName, String argumentsJson) {
        try {
            Skill skill = skills.get(toolName);

            if (skill != null) {
                return skill.execute(argumentsJson);
            }
            return switch (toolName) {
                case ToolDefinitionFactory.UNIT_CONVERTER_TOOL ->
                        executeUnitConverter(argumentsJson);
                case ToolDefinitionFactory.BMI_CALCULATOR_TOOL ->
                        executeBmiCalculator(argumentsJson);
                case ToolDefinitionFactory.HEALTH_PLAN_TOOL ->
                        executeHealthPlan(argumentsJson);
                default -> "未知工具：" + toolName;
            };
        } catch (IllegalArgumentException e) {
            return "工具调用失败：" + e.getMessage();
        } catch (Exception e) {
            return "工具调用失败：参数格式不正确";
        }
    }

    private String executeUnitConverter(String argumentsJson) throws Exception {
        // 单位工具先把中文单位归一化，再调用只认识标准缩写的业务服务。
        JsonNode arguments = objectMapper.readTree(argumentsJson);
        double value = readRequiredDouble(arguments, "value");
        String from = normalizeUnit(arguments, "from");
        String to = normalizeUnit(arguments, "to");

        double result = unitConverterService.convert(value, from, to);
        return String.valueOf(result);
    }

    private String executeBmiCalculator(String argumentsJson) throws Exception {
        // BMI 结果会以 JSON 返回，供模型下一轮调用 health_plan 使用。
        JsonNode arguments = objectMapper.readTree(argumentsJson);
        double heightCm = readRequiredDouble(arguments, "height_cm");
        double weightKg = readRequiredDouble(arguments, "weight_kg");

        HealthToolService.BmiResult result =
                healthToolService.calculateBmi(heightCm, weightKg);
        return objectMapper.writeValueAsString(result);
    }

    private String executeHealthPlan(String argumentsJson) throws Exception {
        // 健康方案只消费 BMI、分类和用户目标，不在这里重复计算 BMI。
        JsonNode arguments = objectMapper.readTree(argumentsJson);
        double bmi = readRequiredDouble(arguments, "bmi");
        String category = readRequiredText(arguments, "category");
        String goal = readOptionalText(arguments, "goal", "保持健康");

        HealthToolService.HealthPlanResult result =
                healthToolService.buildPlan(bmi, category, goal);
        return objectMapper.writeValueAsString(result);
    }

    private double readRequiredDouble(JsonNode arguments, String fieldName) {
        // 不使用 asDouble 的默认 0，避免字段缺失时悄悄产生错误结果。
        JsonNode valueNode = arguments.get(fieldName);
        if (valueNode == null
                || !valueNode.isNumber()
                || !Double.isFinite(valueNode.doubleValue())) {
            throw new IllegalArgumentException(fieldName + " 必须是有限数字");
        }
        return valueNode.doubleValue();
    }

    private String readRequiredText(JsonNode arguments, String fieldName) {
        // 文本参数必须存在且非空，避免把无效分类传给业务层。
        JsonNode valueNode = arguments.get(fieldName);
        if (valueNode == null
                || !valueNode.isTextual()
                || valueNode.asText().isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return valueNode.asText().trim();
    }

    private String readOptionalText(
            JsonNode arguments,
            String fieldName,
            String defaultValue
    ) {
        // 可选参数缺失时使用默认目标，保证工具始终能生成方案。
        JsonNode valueNode = arguments.get(fieldName);
        if (valueNode == null
                || !valueNode.isTextual()
                || valueNode.asText().isBlank()) {
            return defaultValue;
        }
        return valueNode.asText().trim();
    }

    /**
     * 把中文、英文全称统一成 UnitConverterService 支持的标准缩写。
     */
    private String normalizeUnit(JsonNode arguments, String fieldName) {
        JsonNode unitNode = arguments.get(fieldName);
        if (unitNode == null
                || !unitNode.isTextual()
                || unitNode.asText().isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " 不能为空，支持："
                            + ToolDefinitionFactory.SUPPORTED_UNITS
            );
        }

        String unit = unitNode.asText().trim().toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "km", "kilometer", "kilometers", "公里", "千米" -> "km";
            case "m", "meter", "meters", "米" -> "m";
            case "cm", "centimeter", "centimeters", "厘米" -> "cm";
            case "kg", "kilogram", "kilograms", "公斤", "千克" -> "kg";
            case "g", "gram", "grams", "克" -> "g";
            case "h", "hour", "hours", "小时", "时" -> "h";
            case "min", "minute", "minutes", "分钟", "分" -> "min";
            case "s", "second", "seconds", "秒" -> "s";
            default -> throw new IllegalArgumentException(
                    fieldName + " 不支持：" + unit
                            + "，支持：" + ToolDefinitionFactory.SUPPORTED_UNITS
            );
        };
    }
}
