package com.example.demo.speech;

import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.example.demo.util.ConfigUtil;

import java.util.HashMap;

public class SpeechSynthesisService {

    private final AipSpeech client;

    public SpeechSynthesisService() {
        String appId = ConfigUtil.get("baidu.asr.app-id");
        String apiKey = ConfigUtil.get("baidu.asr.api-key");
        String secretKey = ConfigUtil.get("baidu.asr.secret-key");
        this.client = new AipSpeech(appId, apiKey, secretKey);
        System.out.println("[百度TTS] 初始化成功");
    }

    public byte[] synthesize(String text) {
        HashMap<String, Object> options = new HashMap<>();
        options.put("spd", "5");
        options.put("pit", "5");
        options.put("vol", "5");
        options.put("per", "0");

        TtsResponse res = client.synthesis(text, "zh", 1, options);

        byte[] mp3Bytes = res.getData();
        if (mp3Bytes == null) {
            System.err.println("[TTS失败] " + res.getResult().toString());
            return null;
        }

        System.out.println("[TTS成功] mp3大小=" + mp3Bytes.length + " bytes");
        return mp3Bytes;
    }
}