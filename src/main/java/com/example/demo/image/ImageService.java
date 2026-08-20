package com.example.demo.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
// 文生图服务：调用模型创建图片任务，再下载模型返回的图片数据。
public class ImageService {

    private final String apiKey;
    private final String createUrl;
    private final String model;
    private final String size;
    private final boolean promptExtend;
    private final boolean watermark;
    private final String negativePrompt;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public ImageService(
            @Value("${llm.api-key}") String apiKey,
            @Value("${image.create-url}") String createUrl,
            @Value("${image.model}") String model,
            @Value("${image.size:1280*1280}") String size,
            @Value("${image.prompt-extend:true}") boolean promptExtend,
            @Value("${image.watermark:false}") boolean watermark,
            @Value("${image.negative-prompt:}") String negativePrompt
    ) {
        this.apiKey = apiKey;
        this.createUrl = createUrl;
        this.model = model;
        this.size = size;
        this.promptExtend = promptExtend;
        this.watermark = watermark;
        this.negativePrompt = negativePrompt;
    }

    public byte[] generateImage(String prompt) {
        try {
            // 接口返回的是图片 URL 或 data URL，统一转换为字节数组供微信 SDK 发送。
            String imageUrl = createImage(prompt);
            return downloadImage(imageUrl);
        } catch (Exception e) {
            throw new RuntimeException("生图失败：" + e.getMessage(), e);
        }
    }

    private String createImage(String prompt) throws Exception {
        // 按文生图接口要求组装模型、提示词和生成参数。
        Map<String, Object> body = Map.of(
                "model", model,
                "input", Map.of(
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", List.of(
                                                Map.of("text", prompt)
                                        )
                                )
                        )
                ),
                "parameters", Map.of(
                        "size", size,
                        "n", 1,
                        "prompt_extend", promptExtend,
                        "watermark", watermark,
                        "negative_prompt", negativePrompt
                )
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(createUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("创建生图任务失败，状态码：" + response.statusCode() + "，响应：" + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        // 从响应 JSON 的固定路径提取图片地址。
        JsonNode imageNode = root.path("output")
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .path(0)
                .path("image");

        String imageUrl = imageNode.asText("").trim();
        if (imageUrl.isBlank()) {
            throw new RuntimeException("生图成功但没有返回图片URL：" + response.body());
        }

        System.out.println("生图返回URL：" + imageUrl);
        return imageUrl;
    }

    private byte[] downloadImage(String imageUrl) throws Exception {
        // 有些服务返回 data URL，有些返回普通 HTTP URL，因此需要分开处理。
        String normalizedUrl = imageUrl == null ? "" : imageUrl.trim();
        if (normalizedUrl.isBlank()) {
            throw new RuntimeException("图片URL为空");
        }

        if (normalizedUrl.startsWith("data:")) {
            return decodeDataUrl(normalizedUrl);
        }

        Exception httpClientError = null;
        try {
            // 优先使用现代 HttpClient 下载。
            byte[] bytes = downloadWithHttpClient(normalizedUrl);
            if (bytes != null) {
                return bytes;
            }
        } catch (Exception e) {
            httpClientError = e;
        }

        try {
            // 某些图片服务器对 HttpClient 的请求不兼容，使用 URLConnection 作为备用方案。
            byte[] bytes = downloadWithUrlConnection(normalizedUrl);
            if (bytes != null) {
                return bytes;
            }
        } catch (Exception e) {
            if (httpClientError != null) {
                e.addSuppressed(httpClientError);
            }
            throw new RuntimeException("下载图片失败，URL：" + normalizedUrl + "，原因：" + e.getMessage(), e);
        }

        throw new RuntimeException("下载图片失败，URL：" + normalizedUrl);
    }

    private byte[] downloadWithHttpClient(String imageUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "image/*,*/*")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }

        return response.body();
    }

    private byte[] downloadWithUrlConnection(String imageUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(imageUrl).toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept", "image/*,*/*");

        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                return null;
            }

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                return outputStream.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private byte[] decodeDataUrl(String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0 || commaIndex == dataUrl.length() - 1) {
            throw new RuntimeException("data URL 格式不正确");
        }

        String meta = dataUrl.substring(0, commaIndex);
        String data = dataUrl.substring(commaIndex + 1);
        if (meta.contains(";base64")) {
            return Base64.getDecoder().decode(data);
        }

        return data.getBytes(StandardCharsets.UTF_8);
    }
}
