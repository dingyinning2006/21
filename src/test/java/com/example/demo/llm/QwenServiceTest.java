package com.example.demo.llm;

import com.example.demo.tool.HealthToolService;
import com.example.demo.tool.UnitConverterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QwenServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void chainsBmiResultIntoHealthPlanCall() throws Exception {
        StubQwenClient qwenClient = new StubQwenClient();
        qwenClient.add(toolCallResponse(
                "call-bmi",
                ToolDefinitionFactory.BMI_CALCULATOR_TOOL,
                "{\"height_cm\":170,\"weight_kg\":70}"
        ));
        qwenClient.add(toolCallResponse(
                "call-plan",
                ToolDefinitionFactory.HEALTH_PLAN_TOOL,
                "{\"bmi\":24.22,\"category\":\"超重\",\"goal\":\"减脂\"}"
        ));
        qwenClient.add(finalResponse("你的 BMI 是 24.22，建议逐步减脂。"));

        UnitConverterService unitConverterService = new UnitConverterService();
        HealthToolService healthToolService = new HealthToolService();
        QwenService qwenService = new QwenService(
                "qwen-test",
                qwenClient,
                new ToolDefinitionFactory(),
                new ToolExecutor(unitConverterService, healthToolService)
        );

        String reply = qwenService.chat("我的身高170厘米，体重70公斤，帮我制定减脂方案");

        assertEquals("你的 BMI 是 24.22，建议逐步减脂。", reply);
        assertEquals(3, qwenClient.requests.size());

        String secondRequestMessages = qwenClient.requests.get(1).get("messages").toString();
        assertTrue(secondRequestMessages.contains("\"bmi\":24.22"));
        assertTrue(secondRequestMessages.contains("\"category\":\"超重\""));

        String thirdRequestMessages = qwenClient.requests.get(2).get("messages").toString();
        assertTrue(thirdRequestMessages.contains("\"workout\""));
        assertTrue(thirdRequestMessages.contains("\"diet\""));
    }

    private JsonNode toolCallResponse(
            String callId,
            String toolName,
            String arguments
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode message = choices.addObject().putObject("message");
        message.put("role", "assistant");
        message.put("content", "");

        ObjectNode toolCall = message.putArray("tool_calls").addObject();
        toolCall.put("id", callId);
        toolCall.put("type", "function");
        toolCall.putObject("function")
                .put("name", toolName)
                .put("arguments", arguments);
        return root;
    }

    private JsonNode finalResponse(String content) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("choices")
                .addObject()
                .putObject("message")
                .put("role", "assistant")
                .put("content", content);
        return root;
    }

    private static class StubQwenClient extends QwenClient {

        private final Queue<JsonNode> responses = new ArrayDeque<>();
        private final List<Map<String, Object>> requests = new ArrayList<>();

        private StubQwenClient() {
            super("test-key", "http://localhost");
        }

        private void add(JsonNode response) {
            responses.add(response);
        }

        @Override
        public JsonNode send(Map<String, Object> body) {
            requests.add(body);
            return responses.remove();
        }
    }

}
