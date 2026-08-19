package com.example.demo.bot;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 意图识别：用大模型判断用户消息想干什么（查天气 / 普通聊天），
 * 并顺带提取城市名，供天气服务使用。
 */
@Service
public class IntentService {

    private static final String INTENT_PROMPT = "你是意图识别器。判断用户消息的意图，只输出一个 JSON 对象，不要输出任何其他内容。"
            + "格式：{\"intent\":\"weather\"或\"chat\",\"city\":\"城市名或null\"}\n"
            + "判断标准：用户想查天气、问天气、要天气预报时，intent 为 weather，并把城市名填入 city（消息里没提城市就填 null）；"
            + "其他任何情况 intent 都是 chat，city 填 null。";

    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentService(LLMService llmService) {
        this.llmService = llmService;
    }

    /** 意图 + 城市（intent 为 weather 时 city 才有值，可能为 null 表示用默认城市） */
    public record IntentResult(String intent, String city) {
    }

    /** 判断消息意图；模型输出解析失败时按普通聊天处理 */
    public IntentResult classify(String userText) {
        String reply = llmService.askOnce(INTENT_PROMPT, userText);
        try {
            // 模型可能用 ```json 包裹，只截取 { } 之间的部分
            int start = reply.indexOf('{');
            int end = reply.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return new IntentResult("chat", null);
            }
            JsonNode node = objectMapper.readTree(reply.substring(start, end + 1));
            String intent = node.path("intent").isValueNode()
                    ? node.path("intent").asText().trim().toLowerCase()
                    : "chat";
            JsonNode cityNode = node.path("city");
            String city = (!cityNode.isNull() && !cityNode.isMissingNode() && !cityNode.asText().isBlank())
                    ? cityNode.asText().trim()
                    : null;
            return new IntentResult("weather".equals(intent) ? "weather" : "chat", city);
        } catch (Exception e) {
            // 解析失败：当作普通聊天
            return new IntentResult("chat", null);
        }
    }
}
