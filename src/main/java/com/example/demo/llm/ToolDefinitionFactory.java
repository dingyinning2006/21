package com.example.demo.llm;

import org.springframework.stereotype.Component;
import com.example.demo.skill.Skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Function Calling 工具定义工厂。
 *
 * 这里只描述工具的名称、用途和 JSON Schema，不执行具体业务。
 */
@Component
public class ToolDefinitionFactory {

    public static final String UNIT_CONVERTER_TOOL = "unit_converter";
    public static final String BMI_CALCULATOR_TOOL = "bmi_calculator";
    public static final String HEALTH_PLAN_TOOL = "health_plan";
    private final List<Skill> skills;

    public ToolDefinitionFactory(List<Skill> skills) {
        this.skills = skills;
    }
    public static final List<String> SUPPORTED_UNITS =
            List.of("km", "m", "cm", "kg", "g", "h", "min", "s");

    private static final List<String> SUPPORTED_UNIT_INPUTS =
            List.of(
                    "km", "kilometer", "kilometers", "公里", "千米",
                    "m", "meter", "meters", "米",
                    "cm", "centimeter", "centimeters", "厘米",
                    "kg", "kilogram", "kilograms", "公斤", "千克",
                    "g", "gram", "grams", "克",
                    "h", "hour", "hours", "小时", "时",
                    "min", "minute", "minutes", "分钟", "分",
                    "s", "second", "seconds", "秒"
            );

    private static final List<String> BMI_CATEGORIES =
            List.of("偏瘦", "正常", "超重", "肥胖");

    /**
     * 返回本项目当前允许模型调用的全部工具。
     */
    public List<Map<String, Object>> buildTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(createUnitConverterTool());
        tools.add(createBmiCalculatorTool());
        tools.add(createHealthPlanTool());

        for (Skill skill : skills) {
            tools.add(functionTool(
                    skill.getName(),
                    skill.getDescription(),
                    skill.getParameters(),
                    skill.getRequiredParameters()
            ));
        }

        return tools;
    }

    private Map<String, Object> createUnitConverterTool() {
        // enum 既约束模型输出，也为中文单位归一化保留可识别的输入值。
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "value",
                Map.of(
                        "type", "number",
                        "description", "需要转换的数值，例如 5、3.5、100"
                )
        );
        properties.put(
                "from",
                Map.of(
                        "type", "string",
                        "description", "原单位，只能使用标准缩写或常见中文/英文单位名",
                        "enum", SUPPORTED_UNIT_INPUTS
                )
        );
        properties.put(
                "to",
                Map.of(
                        "type", "string",
                        "description", "目标单位，只能使用标准缩写或常见中文/英文单位名",
                        "enum", SUPPORTED_UNIT_INPUTS
                )
        );

        return functionTool(
                UNIT_CONVERTER_TOOL,
                "进行单位换算，例如长度、重量和时间单位之间的转换",
                properties,
                List.of("value", "from", "to")
        );
    }

    private Map<String, Object> createBmiCalculatorTool() {
        // 身高和体重是 BMI 计算所需的两个必填数字参数。
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "height_cm",
                Map.of(
                        "type", "number",
                        "description", "身高，单位厘米"
                )
        );
        properties.put(
                "weight_kg",
                Map.of(
                        "type", "number",
                        "description", "体重，单位公斤"
                )
        );

        return functionTool(
                BMI_CALCULATOR_TOOL,
                "根据身高和体重计算 BMI，并给出体型分类",
                properties,
                List.of("height_cm", "weight_kg")
        );
    }

    private Map<String, Object> createHealthPlanTool() {
        // BMI 和分类来自上一步，goal 可选，用于调整建议方向。
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "bmi",
                Map.of(
                        "type", "number",
                        "description", "上一步 BMI 计算结果"
                )
        );
        properties.put(
                "category",
                Map.of(
                        "type", "string",
                        "description", "体型分类",
                        "enum", BMI_CATEGORIES
                )
        );
        properties.put(
                "goal",
                Map.of(
                        "type", "string",
                        "description", "用户目标，例如 减脂、增肌、保持健康"
                )
        );

        return functionTool(
                HEALTH_PLAN_TOOL,
                "根据 BMI 和体型分类生成锻炼与饮食建议",
                properties,
                List.of("bmi", "category")
        );
    }

    /**
     * 统一包装 OpenAI 兼容格式的 function tool，减少重复 Map 结构。
     */
    private Map<String, Object> functionTool(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> required
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }
}
