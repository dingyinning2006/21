package com.example.demo.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
// 大模型编排服务：管理对话消息、工具定义、工具执行和多轮调用。
public class QwenService {

    // 使用常量避免在不同方法中重复手写 role 和工具名称，减少拼写错误。
    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_TOOL = "tool";

    // 限制连续工具调用轮数，防止模型异常时无限循环请求。
    private static final int MAX_TOOL_ROUNDS = 5;

    private static final String DEFAULT_REPLY = "我暂时没有生成回复。";

    // 这段提示词告诉模型什么时候调用工具，以及 BMI 工具的先后顺序。
    private static final String SYSTEM_PROMPT =
            "你是一个简洁、友好的微信机器人。"
                    + "如果用户需要进行单位换算，请使用 unit_converter 工具。"
                    + "如果用户提供身高和体重，想计算 BMI、判断体型，或者要锻炼/饮食建议，"
                    + "请先使用 bmi_calculator 工具，再根据它返回的 bmi 和 category 使用 health_plan 工具。"
                    + "不要自己猜测计算结果。";

    private final String model;
    private final QwenClient qwenClient;
    private final ToolDefinitionFactory toolDefinitionFactory;
    private final ToolExecutor toolExecutor;

    public QwenService(
            @Value("${llm.model}") String model,
            QwenClient qwenClient,
            ToolDefinitionFactory toolDefinitionFactory,
            ToolExecutor toolExecutor
    ) {
        this.model = model;
        this.qwenClient = qwenClient;
        this.toolDefinitionFactory = toolDefinitionFactory;
        this.toolExecutor = toolExecutor;
    }

    public String chat(String userMessage) {
        // 普通聊天使用项目默认提示词；意图识别等场景可调用下方的自定义提示词入口。
        return chatWithSystemPrompt(SYSTEM_PROMPT, userMessage);
    }

    /**
     * 获取默认的System Prompt，供RAG增强等场景使用
     */
    public String getDefaultSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        try {
            List<Map<String, Object>> messages = buildInitialMessages(systemPrompt, userMessage);
            List<Map<String, Object>> tools = toolDefinitionFactory.buildTools();

            // 每一轮可能是“模型要求调用工具”，也可能是“模型已经生成最终文本”。
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                JsonNode response = sendChatRequest(messages, tools);
                JsonNode assistantMessage = extractAssistantMessage(response);
                JsonNode toolCalls = assistantMessage.path("tool_calls");

                if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                    // 没有 tool_calls，说明模型已经完成回答。
                    return extractAssistantContent(response);
                }

                // assistant 的 tool_calls 必须先放入历史，再追加对应的 tool 结果。
                messages.add(buildAssistantMessage(assistantMessage));
                appendToolMessages(messages, toolCalls);
            }

            return "工具调用次数过多，已停止。";
        } catch (Exception e) {
            e.printStackTrace();
            return "大模型调用异常：" + e.getMessage();
        }
    }

    private List<Map<String, Object>> buildInitialMessages(String systemPrompt, String userMessage) {
        // 每次 chat 都从系统提示词和当前用户问题开始建立消息历史。
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildMessage(ROLE_SYSTEM, systemPrompt));
        messages.add(buildMessage(ROLE_USER, userMessage));
        return messages;
    }

    private Map<String, Object> buildMessage(String role, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private JsonNode extractAssistantMessage(JsonNode response) {
        return response.path("choices").path(0).path("message");
    }

    private String extractAssistantContent(JsonNode response) {
        return response.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText(DEFAULT_REPLY);
    }

    private Map<String, Object> buildAssistantMessage(JsonNode assistantMessage) {
        // 保留模型原始的工具调用信息，第二次请求时模型才能知道自己刚才做了什么。
        Map<String, Object> message = new HashMap<>();
        message.put("role", ROLE_ASSISTANT);
        message.put(
                "content",
                assistantMessage.path("content").isNull()
                        ? null
                        : assistantMessage.path("content").asText()
        );
        message.put("tool_calls", buildToolCallList(assistantMessage.path("tool_calls")));
        return message;
    }

    private List<Map<String, Object>> buildToolCallList(JsonNode toolCalls) {
        // 将模型返回的 JsonNode 转成下一次请求所需的 Java Map 结构。
        List<Map<String, Object>> toolCallList = new ArrayList<>();

        for (JsonNode toolCall : toolCalls) {
            Map<String, Object> function = new HashMap<>();
            function.put("name", toolCall.path("function").path("name").asText());
            function.put("arguments", toolCall.path("function").path("arguments").asText());

            Map<String, Object> toolCallData = new HashMap<>();
            toolCallData.put("id", toolCall.path("id").asText());
            toolCallData.put("type", "function");
            toolCallData.put("function", function);
            toolCallList.add(toolCallData);
        }

        return toolCallList;
    }

    private void appendToolMessages(List<Map<String, Object>> messages, JsonNode toolCalls) {
        // 一轮响应可能包含多个工具调用，必须按每个 call_id 分别追加结果。
        for (JsonNode toolCall : toolCalls) {
            String toolCallId = toolCall.path("id").asText();
            String toolName = toolCall.path("function").path("name").asText();
            String argumentsJson = toolCall.path("function").path("arguments").asText();

            // 工具由本地 Java 代码执行，模型只负责提出调用请求。
            String toolResult = toolExecutor.execute(toolName, argumentsJson);

            System.out.println("调用工具：" + toolName);
            System.out.println("工具参数：" + argumentsJson);
            System.out.println("工具结果：" + toolResult);

            messages.add(buildToolMessage(toolCallId, toolResult));
        }
    }

    private Map<String, Object> buildToolMessage(String toolCallId, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", ROLE_TOOL);
        message.put("tool_call_id", toolCallId);
        message.put("content", content);
        return message;
    }

    private JsonNode sendChatRequest(
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools
    ) throws Exception {
        // 每一轮都携带完整 messages 和工具定义，让模型了解当前对话状态。
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        return sendRequest(body);
    }

    protected JsonNode sendRequest(Map<String, Object> body) throws Exception {
        // 保留这个包装方法，便于测试时重写并模拟模型响应。
        return qwenClient.send(body);
    }

}
