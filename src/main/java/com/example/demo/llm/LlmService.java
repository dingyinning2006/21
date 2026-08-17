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

public class LlmService {
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 构造方法手动传参
    public LlmService(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    // 原有纯文本对话方法，不动，兼容之前所有调用
    public String chat(String userQuestion) throws IOException {
        return chat(userQuestion, null);
    }

    /**
     * 图文混合对话核心方法
     * @param userText 用户文字提问，无文字传null
     * @param imageUrl 微信图片公网链接，无图传null
     * @return AI回答文本
     * @throws IOException http请求异常
     */
    @SuppressWarnings("unchecked")
    public String chat(String userText, String imageUrl) throws IOException {
        URL url = URI.create(baseUrl + "/chat/completions").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        // 构建图文混合content数组
        List<Object> contentList = new ArrayList<>();
        // 添加文字部分
        if (userText != null && !userText.isBlank()) {
            Map<String, String> textItem = Map.of("type", "text", "text", userText);
            contentList.add(textItem);
        }
        // 添加图片部分
        if (imageUrl != null && !imageUrl.isBlank()) {
            Map<String, Object> imageItem = Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", imageUrl)
            );
            contentList.add(imageItem);
        }

        // 组装请求体
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
}
