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

import java.util.Base64;


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

    private final String visionModel;

    public LLMService(@Value("${llm.api-key}") String apiKey,
                      @Value("${llm.base-url}") String baseUrl,
                      @Value("${llm.model}") String model,@Value("${llm.vision-model}") String visionModel) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.visionModel = visionModel;
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

        String reply = callApi(requestBody);

      // 5. 把模型回答也存进历史，下次就有上下文了
        history.addLast(new ChatMessage("assistant", reply));
        return reply;

    }

    /** 识别图片内容，返回文字描述 */
    public String describeImage(byte[] imageBytes, String userText) {
        // 根据图片文件头判断真实格式，避免 MIME 写错被接口拒绝
        String mime = detectImageMime(imageBytes);
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        // 视觉请求的 content 是数组：图片 + 文字提示
        List<Object> content = new ArrayList<>();
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)));
        content.add(Map.of("type", "text", "text",
                (userText == null || userText.isBlank()) ? "请描述这张图片的内容" : userText));

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", visionModel,
                "messages", List.of(Map.of("role", "user", "content", content))
        ));
        return callApi(requestBody);
    }

    /** 公共的 HTTP 调用 + 解析，chat() 和 describeImage() 共用 */
    private String callApi(String requestBody) {
        String responseBody = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    /** 看图片文件头（魔数）判断格式，微信发的图大多是 JPEG */
    private String detectImageMime(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return "image/jpeg"; // 默认按 JPEG 处理
    }


    /** 一条对话消息：role 是 user/assistant/system */
    record ChatMessage(String role, String content) {
    }


}
