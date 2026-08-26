package com.example.demo.llm;

import com.example.demo.tool.HealthToolService;
import com.example.demo.tool.UnitConverterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutorTest {

    private final ToolExecutor toolExecutor = new ToolExecutor(
            new UnitConverterService(),
            new HealthToolService(),
            List.of()
    );

    @Test
    void convertsChineseUnitsBeforeCallingUnitService() {
        // 验证模型传入中文单位时，执行器会先完成单位归一化。
        String result = toolExecutor.execute(
                ToolDefinitionFactory.UNIT_CONVERTER_TOOL,
                "{\"value\":60,\"from\":\"分钟\",\"to\":\"秒\"}"
        );

        assertEquals("3600.0", result);
    }

    @Test
    void returnsBmiAsJson() {
        // 工具结果必须是 JSON，模型才能把 BMI 和分类传给下一步工具。
        String result = toolExecutor.execute(
                ToolDefinitionFactory.BMI_CALCULATOR_TOOL,
                "{\"height_cm\":170,\"weight_kg\":70}"
        );

        assertTrue(result.contains("\"bmi\":24.22"));
        assertTrue(result.contains("\"category\":\"超重\""));
    }

    @Test
    void returnsReadableErrorWhenRequiredArgumentIsMissing() {
        // 缺少参数时应该返回可读错误，而不是静默使用 0。
        String result = toolExecutor.execute(
                ToolDefinitionFactory.UNIT_CONVERTER_TOOL,
                "{\"from\":\"min\",\"to\":\"s\"}"
        );

        assertEquals("工具调用失败：value 必须是有限数字", result);
    }
}
