package com.example.demo.llm;

import com.example.demo.tool.HealthToolService;
import com.example.demo.tool.UnitConverterService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutorTest {

    private final ToolExecutor toolExecutor = new ToolExecutor(
            new UnitConverterService(),
            new HealthToolService()
    );

    @Test
    void convertsChineseUnitsBeforeCallingUnitService() {
        String result = toolExecutor.execute(
                ToolDefinitionFactory.UNIT_CONVERTER_TOOL,
                "{\"value\":60,\"from\":\"分钟\",\"to\":\"秒\"}"
        );

        assertEquals("3600.0", result);
    }

    @Test
    void returnsBmiAsJson() {
        String result = toolExecutor.execute(
                ToolDefinitionFactory.BMI_CALCULATOR_TOOL,
                "{\"height_cm\":170,\"weight_kg\":70}"
        );

        assertTrue(result.contains("\"bmi\":24.22"));
        assertTrue(result.contains("\"category\":\"超重\""));
    }

    @Test
    void returnsReadableErrorWhenRequiredArgumentIsMissing() {
        String result = toolExecutor.execute(
                ToolDefinitionFactory.UNIT_CONVERTER_TOOL,
                "{\"from\":\"min\",\"to\":\"s\"}"
        );

        assertEquals("工具调用失败：value 必须是有限数字", result);
    }
}
