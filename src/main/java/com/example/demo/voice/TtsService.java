package com.example.demo.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class TtsService {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final String voice;
    private final String format;
    private final int sampleRate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TtsService(
            @Value("${llm.api-key}") String apiKey,
            @Value("${tts.api-url}") String apiUrl,
            @Value("${tts.model}") String model,
            @Value("${tts.voice}") String voice,
            @Value("${tts.format}") String format,
            @Value("${tts.sample-rate}") int sampleRate
    ) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.voice = voice;
        this.format = format;
        this.sampleRate = sampleRate;
    }

    public byte[] synthesize(String text) {
        try {
            String audioUrl = createAudio(text);
            return downloadAudio(audioUrl);
        } catch (Exception e) {
            throw new RuntimeException("语音合成失败：" + e.getMessage(), e);
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }

    private String createAudio(String text) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", Map.of(
                        "text", text,
                        "voice", voice,
                        "format", format,
                        "sample_rate", sampleRate
                )
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("创建语音失败，状态码：" + response.statusCode() + "，响应：" + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());

        String audioUrl = root.path("output")
                .path("audio")
                .path("url")
                .asText();

        if (audioUrl == null || audioUrl.isBlank()) {
            throw new RuntimeException("语音合成成功但没有返回音频 URL：" + response.body());
        }

        return audioUrl;
    }

    private byte[] downloadAudio(String audioUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(audioUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("下载音频失败，状态码：" + response.statusCode());
        }

        return response.body();
    }
}
