package com.example.demo.llm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LlmService  {


    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 构造方法手动传参（保持不变）
    public  LlmService (String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    // ========== 原有方法，不动 ==========

    public String chat(String userQuestion) throws IOException {
        return chat(userQuestion, null);
    }

    @SuppressWarnings("unchecked")
    public String chat(String userText, String imageUrl) throws IOException {
        URL url = URI.create(baseUrl + "/chat/completions").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        List<Object> contentList = new ArrayList<>();
        if (userText != null && !userText.isBlank()) {
            Map<String, String> textItem = Map.of("type", "text", "text", userText);
            contentList.add(textItem);
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            Map<String, Object> imageItem = Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", imageUrl)
            );
            contentList.add(imageItem);
        }

        Map<String, Object> reqBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", contentList)
                ),
                "temperature", 0.7
        );

        String jsonBody = objectMapper.writeValueAsString(reqBody);
        conn.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("LLM接口错误，响应码:" + code);
        }
        String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> respMap = objectMapper.readValue(resp, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");
    }

    // ========== 新增：支持 Function Calling 的方法 ==========

    /**
     * 支持工具调用的对话方法
     * @param messages 完整的消息列表（包含 system/user/assistant/tool）
     * @param tools 工具定义列表（JSON Schema），为 null 时不启用工具调用
     * @return 包含 content 和 tool_calls 的 Map
     *         - content: LLM 的文本回答（可能为 null）
     *         - tool_calls: 工具调用列表（可能为 null），每个元素包含 id/type/function
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> chatWithTools(List<Map<String, Object>> messages,
                                             List<Map<String, Object>> tools) throws IOException {
        URL url = URI.create(baseUrl + "/chat/completions").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        // 构建请求体：用 ArrayList 因为 Map.of 不支持 null 值
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("model", model);
        reqBody.put("messages", messages);
        reqBody.put("temperature", 0.7);
        if (tools != null && !tools.isEmpty()) {
            reqBody.put("tools", tools);
            reqBody.put("tool_choice", "auto");
        }

        String jsonBody = objectMapper.writeValueAsString(reqBody);
        conn.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("LLM接口错误，响应码:" + code);
        }
        String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> respMap = objectMapper.readValue(resp, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");

        // 提取 content
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", msg.get("content"));

        // 提取 tool_calls（关键：LLM 决定调用工具时会返回这个字段）
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) msg.get("tool_calls");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            result.put("tool_calls", toolCalls);
        }

        return result;
    }
}
