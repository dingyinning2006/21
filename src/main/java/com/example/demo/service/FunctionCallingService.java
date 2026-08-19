package com.example.demo.service;

import com.example.demo.llm.LlmService;
import com.example.demo.tool.Tool;
import com.example.demo.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class FunctionCallingService {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private LlmService llmService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ITERATIONS = 5;

    /**
     * 处理用户消息（无历史对话）
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * 处理用户消息（带历史对话）
     * 注意：chatHistory 类型统一用 List<Map<String, Object>>
     */
    public String chat(String userMessage, List<Map<String, Object>> chatHistory) {
        // 1. 构建消息列表
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统提示词
        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个智能微信助手，可以使用提供的工具来回答用户问题。" +
                "当用户的问题需要实时信息、数学计算或单位换算时，请调用相应的工具。" +
                "如果不需要工具，请直接用自然语言回答。回答要简洁友好。");
        messages.add(systemMsg);

        // 历史消息（类型一致，直接 addAll）
        if (chatHistory != null) {
            messages.addAll(chatHistory);
        }

        // 当前用户消息
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        // 2. 获取工具定义
        List<Map<String, Object>> tools = toolRegistry.getAllToolSchemas();
        System.out.println("[FunctionCalling] 已加载 " + tools.size() + " 个工具");

        // 3. 多轮工具调用循环
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("[FunctionCalling] 第 " + (i + 1) + " 轮调用 LLM...");

            Map<String, Object> llmResponse;
            try {
                llmResponse = llmService.chatWithTools(messages, tools);
            } catch (IOException e) {
                System.err.println("[FunctionCalling] LLM 调用失败：" + e.getMessage());
                return "抱歉，AI 服务暂时不可用，请稍后再试。";
            }

            String content = (String) llmResponse.get("content");
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) llmResponse.get("tool_calls");

            // 没有工具调用，直接返回答案
            if (toolCalls == null || toolCalls.isEmpty()) {
                System.out.println("[FunctionCalling] LLM 直接回答，无需调用工具");
                return content != null ? content : "抱歉，我无法回答这个问题。";
            }

            // 把 assistant 消息加入历史
            Map<String, Object> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", content);
            assistantMsg.put("tool_calls", toolCalls);
            messages.add(assistantMsg);

            System.out.println("[FunctionCalling] LLM 决定调用 " + toolCalls.size() + " 个工具");

            // 执行所有工具
            for (Map<String, Object> toolCall : toolCalls) {
                String callId = (String) toolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String toolName = (String) function.get("name");
                String argumentsStr = (String) function.get("arguments");

                System.out.println("[FunctionCalling] 调用工具：" + toolName + "，参数：" + argumentsStr);

                Map<String, Object> arguments = parseArguments(argumentsStr);
                String toolResult = executeTool(toolName, arguments);

                System.out.println("[FunctionCalling] 工具 " + toolName + " 执行结果：" + toolResult);

                // 工具结果以 role=tool 回传
                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", callId);
                toolMsg.put("content", toolResult);
                messages.add(toolMsg);
            }
        }

        System.out.println("[FunctionCalling] 达到最大调用轮次 " + MAX_ITERATIONS);
        return "处理超时，请稍后再试。";
    }

    private Map<String, Object> parseArguments(String argumentsStr) {
        if (argumentsStr == null || argumentsStr.isEmpty()) {
            return new HashMap<>();
        }
        try {
            JsonNode root = objectMapper.readTree(argumentsStr);
            return objectMapper.convertValue(root, Map.class);
        } catch (Exception e) {
            System.err.println("[FunctionCalling] 参数解析失败：" + e.getMessage());
            return new HashMap<>();
        }
    }

    private String executeTool(String toolName, Map<String, Object> arguments) {
        Tool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", "未找到工具：" + toolName);
            return toJson(error);
        }
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", "工具执行异常：" + e.getMessage());
            return toJson(error);
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\":\"json序列化失败\"}";
        }
    }
}