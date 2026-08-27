package com.example.demo.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * 千问 HTTP 客户端。
 *
 * 这个类只负责网络通信，不负责判断是否调用工具，也不负责执行业务工具。
 */
@Component
public class QwenClient {

    private final String apiKey;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public QwenClient(
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.base-url}") String baseUrl
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * 发送一轮聊天请求，并把 JSON 响应解析成 JsonNode。
     */
    public JsonNode send(Map<String, Object> body) throws Exception {
        String requestBody = objectMapper.writeValueAsString(body);

        System.out.println("\n发送给千问：");
        System.out.println(requestBody);

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
            throw new RuntimeException(
                    "HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        System.out.println("\n千问返回：");
        System.out.println(response.body());

        return objectMapper.readTree(response.body());
    }
}
