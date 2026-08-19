package com.example.demo.bot;

import io.github.kasukusakura.silkcodec.SilkCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 百炼语音合成（TTS）：把文字合成 wav 音频，再转成微信语音气泡用的 silk 格式。
 * 调用流程：POST SpeechSynthesizer 拿到音频 URL -> GET 下载 wav -> 提取 PCM -> SilkCoder 编码 silk。
 * 注意：v3 系列音色的名字必须带版本号（如 longanhuan_v3.6），用旧名会报 418。
 */
@Service
public class TTSService {

    private static final Logger logger = LoggerFactory.getLogger(TTSService.class);

    private final String apiKey;
    private final String workspaceId;
    private final String model;
    private final String voice;
    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TTSService(@Value("${llm.api-key}") String apiKey,
                      @Value("${llm.workspace-id}") String workspaceId,
                      @Value("${llm.tts-model}") String model,
                      @Value("${llm.tts-voice}") String voice) {
        this.apiKey = apiKey;
        this.workspaceId = workspaceId;
        this.model = model;
        this.voice = voice;
    }

    /** 合成语音，返回 wav 音频字节；失败抛异常由调用方兜底 */
    public byte[] synthesize(String text) throws Exception {
        String url = "https://" + workspaceId + ".cn-beijing.maas.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer";

        // 1. 请求合成，非流式返回的 audio.url 指向一个临时 wav 文件
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "input", Map.of("text", text, "voice", voice, "format", "wav", "sample_rate", 24000)
        ));
        String responseBody = restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(responseBody);
        String audioUrl = root.path("output").path("audio").path("url").asText();
        if (audioUrl == null || audioUrl.isBlank()) {
            throw new IllegalStateException("TTS 响应里没有 audio.url：" + responseBody);
        }

        // 2. 下载 wav 字节
        byte[] wav = restClient.get().uri(audioUrl).retrieve().body(byte[].class);
        logger.info("TTS 合成成功：{} 字符 -> {} 字节音频", text.length(), wav.length);
        return wav;
    }

    /** 合成语音并转成微信语音气泡用的 silk，返回（silk 字节, 播放时长毫秒） */
    public SilkResult synthesizeSilk(String text) throws Exception {
        byte[] wav = synthesize(text);
        return wavToSilk(wav);
    }

    /** 解析 wav（16 位单声道 PCM）-> SilkCoder 编码成 silk */
    private SilkResult wavToSilk(byte[] wav) throws Exception {
        // 1. 按 chunk 解析 wav：找 fmt 和 data 块，兼容非标准 44 字节头
        int dataOffset = -1;
        int dataLength = 0;
        int sampleRate = 0;
        int offset = 12; // 跳过 "RIFF" + 长度 + "WAVE"
        while (offset + 8 <= wav.length) {
            String chunkId = new String(wav, offset, 4, StandardCharsets.US_ASCII);
            int chunkSize = readIntLE(wav, offset + 4);
            if ("fmt ".equals(chunkId)) {
                int format = readShortLE(wav, offset + 8);
                int channels = readShortLE(wav, offset + 10);
                sampleRate = readIntLE(wav, offset + 12);
                int bits = readShortLE(wav, offset + 22);
                if (format != 1 || channels != 1 || bits != 16) {
                    throw new IllegalStateException(
                            "只支持 16 位单声道 PCM wav，实际 format=" + format + ", channels=" + channels + ", bits=" + bits);
                }
            } else if ("data".equals(chunkId)) {
                dataOffset = offset + 8;
                // OSS 生成的 wav 里 data 块 size 字段是占位大数，不准确；以文件实际剩余字节为准
                dataLength = Math.min(chunkSize, wav.length - dataOffset);
                break;
            }
            offset += 8 + chunkSize + (chunkSize % 2); // chunk 数据按 2 字节对齐
        }
        if (dataOffset < 0 || dataLength <= 0) {
            throw new IllegalStateException("wav 里没有 data 块");
        }

        // 2. PCM -> silk 编码（silk-codec 的 native 库随 jar 打包，自动加载）
        ByteArrayOutputStream silkOut = new ByteArrayOutputStream();
        SilkCoder.encode(new ByteArrayInputStream(wav, dataOffset, dataLength),
                silkOut, sampleRate, sampleRate);
        byte[] silk = silkOut.toByteArray();

        // 3. 时长：采样数 / 采样率（16 位 = 每采样 2 字节）
        long playtime = dataLength / 2L * 1000L / sampleRate;
        logger.info("wav -> silk 转换成功：{} 字节 -> {} 字节，时长 {}ms", wav.length, silk.length, playtime);
        return new SilkResult(silk, (int) playtime);
    }

    /** 小端序读取 int（wav 头用） */
    private int readIntLE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    /** 小端序读取 short */
    private int readShortLE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    /** silk 编码结果：字节 + 播放时长 */
    public record SilkResult(byte[] silk, int playtimeMillis) {
    }

}
