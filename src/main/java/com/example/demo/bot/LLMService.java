package com.example.demo.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LLMService {

    private static final String SYSTEM_PROMPT = "你是一个微信智能助理，请用简洁、友好、准确的中文回答用户的问题。";

    /** 每个用户最多保留的对话条数（user/assistant 各算一条），防止内存无限增长 */
    private static final int MAX_HISTORY = 10;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** userId -> 该用户的对话历史（最近的 MAX_HISTORY 条） */
    private final Map<String, Deque<ChatMessage>> conversations = new ConcurrentHashMap<>();

    public LLMService(@Value("${llm.api-key}") String apiKey,
                      @Value("${llm.base-url}") String baseUrl,
                      @Value("${llm.model}") String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    /** 让模型结合该用户的历史 + 本条消息，生成回复 */
    public String chat(String userId, String userText) {
        Deque<ChatMessage> history = conversations.computeIfAbsent(userId, k -> new ArrayDeque<>());

        // 1. 把新消息塞进历史，超过上限就丢最旧的
        history.addLast(new ChatMessage("user", userText));
        while (history.size() > MAX_HISTORY) {
            history.pollFirst();
        }

        // 2. 组装请求：system 提示词 + 该用户的历史对话
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        messages.addAll(history);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", messages
        ));

        // 3. 调百炼的 OpenAI 兼容接口
        String responseBody = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        // 4. 从响应里挖出 choices[0].message.content
        JsonNode root = objectMapper.readTree(responseBody);
        String reply = root.path("choices").path(0).path("message").path("content").asText();

        // 5. 把模型回答也存进历史，下次就有上下文了
        history.addLast(new ChatMessage("assistant", reply));
        return reply;
    }

    /** 一条对话消息：role 是 user/assistant/system */
    record ChatMessage(String role, String content) {
    }
}
