package com.example.demo.audio.tts;

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
// 文本转语音服务：创建音频任务并下载最终音频文件。
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
        // 对外只返回音频字节，隐藏“调用接口获取 URL + 下载音频”的细节。
        try {
            // 对上层隐藏“创建任务 + 下载文件”两个网络步骤。
            String audioUrl = createAudio(text);
            return downloadAudio(audioUrl);
        } catch (Exception e) {
            throw new RuntimeException("语音合成失败：" + e.getMessage(), e);
        }
    }

    private String createAudio(String text) throws Exception {
        // 组装 TTS 模型需要的文本、音色、格式和采样率参数。
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

        // 从接口响应中提取音频 URL，下一步再下载二进制内容。
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
        // 创建语音接口只返回地址，这里负责把地址转换成二进制音频。
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
