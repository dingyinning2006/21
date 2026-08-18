package com.example.demo.intent;

import com.example.demo.llm.QwenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class IntentService {

    private final QwenService qwenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentService(QwenService qwenService) {
        this.qwenService = qwenService;
    }

    public IntentResult recognize(String userText) {
        String ruleType = guessReplyType(userText);

        if (!"text".equals(ruleType)) {
            IntentResult ruleResult = new IntentResult();
            ruleResult.setReplyType(ruleType);
            ruleResult.setUserQuestion(cleanUserQuestion(ruleType, userText));
            return ruleResult;
        }

        String systemPrompt = """
                你是一个意图识别器。
                你只负责判断用户希望机器人用什么形式回复。
                只能返回 JSON，不要返回解释，不要使用 Markdown。

                replyType 只能是以下四种之一：
                text：用户需要文字回答
                image：用户想要图片、画图、生成图片
                voice：用户想要语音、朗读、音频回复
                weather：用户想要查询天气

                userQuestion 字段要返回用户真正想处理的内容，不要保留命令词。

                示例1：
                用户输入：用语音读一下：你好，我是你的机器人
                返回：{"replyType":"voice","userQuestion":"你好，我是你的机器人"}

                示例2：
                用户输入：帮我画一只可爱的猫
                返回：{"replyType":"image","userQuestion":"一只可爱的猫"}

                示例3：
                用户输入：介绍一下 Spring Boot
                返回：{"replyType":"text","userQuestion":"介绍一下 Spring Boot"}

                示例4：
                用户输入：北京今天的天气怎么样
                返回：{"replyType":"weather","userQuestion":"北京今天的天气怎么样"}
                """;

        String result = qwenService.chatWithSystemPrompt(systemPrompt, userText);

        try {
            IntentResult intent = objectMapper.readValue(result, IntentResult.class);
            normalizeIntent(intent, userText);
            return intent;
        } catch (Exception e) {
            IntentResult fallback = new IntentResult();
            fallback.setReplyType("text");
            fallback.setUserQuestion(userText);
            return fallback;
        }
    }

    private void normalizeIntent(IntentResult intent, String originalText) {
        if (intent.getReplyType() == null || intent.getReplyType().isBlank()) {
            intent.setReplyType("text");
        }

        if (intent.getUserQuestion() == null || intent.getUserQuestion().isBlank()) {
            intent.setUserQuestion(originalText);
        }

        intent.setUserQuestion(cleanUserQuestion(intent.getReplyType(), intent.getUserQuestion()));
    }

    private String guessReplyType(String text) {
        if (containsAny(text, "语音", "朗读", "读一下", "念一下", "音频")) {
            return "voice";
        }

        if (containsAny(text, "天气", "气温", "温度", "下雨", "晴", "风力", "空气质量", "预报")) {
            return "weather";
        }

        if (containsAny(text, "画", "图片", "图像", "生图", "生成图", "绘制", "照片", "海报", "头像")) {
            return "image";
        }

        return "text";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String cleanUserQuestion(String replyType, String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim();

        if ("voice".equals(replyType)) {
            cleaned = removePrefixes(cleaned,
                    "用语音读一下：",
                    "用语音读一下:",
                    "语音读一下：",
                    "语音读一下:",
                    "朗读一下：",
                    "朗读一下:",
                    "朗读：",
                    "朗读:",
                    "读一下：",
                    "读一下:",
                    "念一下：",
                    "念一下:"
            );
        }

        if ("image".equals(replyType)) {
            cleaned = removePrefixes(cleaned,
                    "帮我画一张",
                    "帮我画一个",
                    "帮我画",
                    "画一张",
                    "画一个",
                    "画",
                    "生成一张",
                    "生成一个",
                    "生成图片：",
                    "生成图片:",
                    "生图：",
                    "生图:"
            );
        }

        if ("weather".equals(replyType)) {
            cleaned = removePrefixes(cleaned,
                    "帮我查一下",
                    "帮我看看",
                    "查一下",
                    "看看",
                    "查询",
                    "告诉我",
                    "请问"
            );
        }

        return cleaned.trim();
    }

    private String removePrefixes(String text, String... prefixes) {
        String cleaned = text;

        for (String prefix : prefixes) {
            cleaned = removePrefix(cleaned, prefix);
        }

        return cleaned;
    }

    private String removePrefix(String text, String prefix) {
        if (text.startsWith(prefix)) {
            return text.substring(prefix.length()).trim();
        }

        return text;
    }
}
