package com.example.demo.audio.speech;

import com.example.demo.audio.codec.SilkConverterService;
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
// 语音识别服务：将微信 SILK 语音转成 WAV，再交给多模态模型转写文字。
public class VoiceTranscriptionService {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int sampleRate;
    private final SilkConverterService silkConverterService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public VoiceTranscriptionService(
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${speech.model}") String model,
            @Value("${tts.sample-rate}") int sampleRate,
            SilkConverterService silkConverterService
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.sampleRate = sampleRate;
        this.silkConverterService = silkConverterService;
    }

    public String transcribeSilk(byte[] silkBytes) {
        // 这是微信语音进入系统的入口，负责衔接 SILK 解码和模型转写。
        // 模型接口不直接接收微信 SILK，所以先完成格式转换。
        byte[] wavBytes = silkConverterService.convertSilkToWav(silkBytes, sampleRate);
        return transcribeWav(wavBytes);
    }

    private String transcribeWav(byte[] wavBytes) {
        try {
            // 识别接口接收 WAV data URL，音频内容嵌入 JSON 后发送。
            // 用 Base64 data URL 把 WAV 音频嵌入请求 JSON。
            String base64Audio = Base64.getEncoder().encodeToString(wavBytes);
            String audioDataUrl = "data:audio/wav;base64," + base64Audio;

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "input_audio",
                                                    "input_audio", Map.of("data", audioDataUrl)
                                            ),
                                            Map.of(
                                                    "type", "text",
                                                    "text", "请把这段语音转写成简体中文文字，只输出转写结果，不要解释。"
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
                return "语音识别失败，状态码：" + response.statusCode() + "，响应：" + response.body();
            }

            JsonNode root = objectMapper.readTree(response.body());

            // 只返回模型转写出的正文，去掉首尾空白。
            return root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("")
                    .trim();
        } catch (Exception e) {
            return "语音识别异常：" + e.getMessage();
        }
    }
}
