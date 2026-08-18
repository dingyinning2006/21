package com.example.demo;

import com.example.demo.speech.SpeechSynthesisService;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TtsTest {
    public static void main(String[] args) throws Exception {
        SpeechSynthesisService tts = new SpeechSynthesisService();
        byte[] mp3 = tts.synthesize("你好，我是你的微信助手，今天天气真好");
        if (mp3 != null) {
            Files.write(Paths.get("tts_test.mp3"), mp3);
            System.out.println("✅ 已生成 tts_test.mp3，大小=" + mp3.length + " bytes");
        } else {
            System.out.println("❌ TTS 失败");
        }
    }
}
