
package com.example.demo.audio.speech;

import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 语音转文字服务
 */
@Service
public class SpeechToTextService {

    /**
     * 将语音文件转换为文字
     * @param audioPath 语音文件路径
     * @param format 语音格式 (silk, wav等)
     * @return 转换后的文字
     * @throws SpeechToTextException 转换失败时抛出
     */
    public String convertToText(String audioPath, String format) throws SpeechToTextException {
        // 这里应该是实际的语音转文字逻辑
        // 为了演示目的，我们模拟实现

        // 模拟处理延迟
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 模拟语音识别结果
        if (audioPath.contains("error")) {
            throw new SpeechToTextException("语音识别失败: 无法解析音频文件");
        }

        // 模拟不同的语音识别结果
        if (audioPath.contains("pressure")) {
            return "我今天压力很大，工作很多，睡眠不太好。";
        } else if (audioPath.contains("sleep")) {
            return "我昨晚睡得很好，做了三个任务，遇到了一些困难。";
        } else {
            return "我今天完成了所有任务，压力不大，睡眠质量也不错。";
        }
    }

    /**
     * 自定义语音转文字异常
     */
    public static class SpeechToTextException extends Exception {
        public SpeechToTextException(String message) {
            super(message);
        }
    }
}
