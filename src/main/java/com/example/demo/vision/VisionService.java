package com.example.demo.vision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
// 视觉理解服务：把微信图片编码后发送给多模态模型，并返回文字描述。
public class VisionService {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public VisionService(
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${vision.model}") String model
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String understandImage(byte[] imageBytes, String prompt) {
        try {
            // API 接收 data URL，因此先把二进制图片编码为 Base64 字符串。
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String imageUrl = "data:image/jpeg;base64," + base64Image;

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "image_url",
                                                    "image_url", Map.of("url", imageUrl)
                                            ),
                                            Map.of(
                                                    "type", "text",
                                                    "text", prompt
                                            )
                                    )
                            )
                    )
            );

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "图片理解失败，状态码：" + response.statusCode() + "，响应：" + response.body();
            }

            JsonNode root = objectMapper.readTree(response.body());

            // 视觉模型的回答仍然位于标准 chat completion 的 message.content 中。
            return root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("我暂时没有看懂这张图片。");
        } catch (Exception e) {
            return "图片理解异常：" + e.getMessage();
        }
    }
}
