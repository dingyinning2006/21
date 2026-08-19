package com.example.demo.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Base64;


@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    private static final String SYSTEM_PROMPT = "你是一个微信智能助理，请用简洁、友好、准确的中文回答用户的问题。";

    /** 每个用户最多保留的对话条数（user/assistant 各算一条），防止内存无限增长 */
    private static final int MAX_HISTORY = 10;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** userId -> 该用户的对话历史（最近的 MAX_HISTORY 条） */
    private final Map<String, Deque<ChatMessage>> conversations = new ConcurrentHashMap<>();

    private final String visionModel;

    public LLMService(@Value("${llm.api-key}") String apiKey,
                      @Value("${llm.base-url}") String baseUrl,
                      @Value("${llm.model}") String model,@Value("${llm.vision-model}") String visionModel) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.visionModel = visionModel;
    }

    /** 让模型结合该用户的历史 + 本条消息，生成回复 */
    public String chat(String userId, String userText) {
        Deque<ChatMessage> history = conversations.computeIfAbsent(userId, k -> new ArrayDeque<>());

        // 1. 把新消息塞进历史，超过上限就丢最旧的
        history.addLast(new ChatMessage("user", userText));
        while (history.size() > MAX_HISTORY) {
            history.pollFirst();
        }

        // 2. 组装请求：system 提示词 + 该用户的历史对话
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        messages.addAll(history);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", messages
        ));

        String reply = callApi(requestBody);

      // 5. 把模型回答也存进历史，下次就有上下文了
        history.addLast(new ChatMessage("assistant", reply));
        return reply;

    }

    /** 单轮对话（不带历史记忆），用于意图识别等简单场景 */
    public String askOnce(String systemPrompt, String userText) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        messages.add(new ChatMessage("user", userText));
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", messages
        ));
        return callApi(requestBody);
    }

    /** 工具执行器：由业务方实现，根据工具名和参数 JSON 执行并返回文本结果 */
    public interface ToolExecutor {
        String execute(String toolName, JsonNode args) throws Exception;
    }

    /** 工具调用循环的最大轮数，防止模型无限调工具 */
    private static final int MAX_TOOL_TURNS = 5;

    /** 带 Function Calling 的对话：模型需要工具时自动调用执行器，把结果回填后再回答。结果同样记入多轮记忆。 */
    public String chatWithTools(String userId, String userText, ToolExecutor executor) {
        Deque<ChatMessage> history = conversations.computeIfAbsent(userId, k -> new ArrayDeque<>());
        history.addLast(new ChatMessage("user", userText));
        while (history.size() > MAX_HISTORY) {
            history.pollFirst();
        }

        // 组装本轮 messages：system + 历史 + 工具定义。工具消息只在循环内拼，不污染历史。
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (ChatMessage m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }

        String content = null;
        for (int turn = 0; turn < MAX_TOOL_TURNS; turn++) {
            String responseBody = postChat(messages, TOOLS);
            JsonNode message = objectMapper.readTree(responseBody).path("choices").path(0).path("message");
            JsonNode toolCalls = message.path("tool_calls");

            // 模型没有要调用工具 -> 这就是最终回答
            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                content = message.path("content").asText();
                break;
            }

            // 模型要调用工具：先把 assistant 消息（含 tool_calls）原样放回，再逐个执行并回填结果
            messages.add(objectMapper.convertValue(message, Map.class));
            for (JsonNode toolCall : toolCalls) {
                String toolName = toolCall.path("function").path("name").asText();
                String result;
                try {
                    JsonNode args = objectMapper.readTree(toolCall.path("function").path("arguments").asText());
                    result = executor.execute(toolName, args);
                    logger.info("工具调用成功：{}，参数={}", toolName, toolCall.path("function").path("arguments").asText());
                } catch (Exception e) {
                    logger.warn("工具执行失败：{}，原因：{}", toolName, e.getMessage());
                    result = "工具执行失败：" + e.getMessage();
                }
                messages.add(Map.of("role", "tool", "tool_call_id", toolCall.path("id").asText(), "content", result));
            }
        }
        if (content == null) {
            content = "抱歉，工具调用次数过多，请稍后再试。";
        }

        history.addLast(new ChatMessage("assistant", content));
        return content;
    }

    /** 两个工具的 JSON Schema 定义：天气 + 当前时间 */
    private static final List<Map<String, Object>> TOOLS = List.of(
            Map.of("type", "function", "function", Map.of(
                    "name", "get_weather",
                    "description", "查询指定城市的当前天气情况，包括温度、天气现象、风力等",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of("city", Map.of(
                                    "type", "string",
                                    "description", "城市名，如：北京、扬州、上海")),
                            "required", List.of("city")))),
            Map.of("type", "function", "function", Map.of(
                    "name", "get_current_time",
                    "description", "获取当前的日期和时间",
                    "parameters", Map.of("type", "object", "properties", Map.of()))),
            Map.of("type", "function", "function", Map.of(
                    "name", "calculate",
                    "description", "计算数学表达式的结果，支持加减乘除、括号、幂运算。例：3.5*(2+4)/7、2^10",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of("expression", Map.of(
                                    "type", "string",
                                    "description", "数学表达式（字符串形式），如：3.5*(2+4)/7")),
                            "required", List.of("expression")))),
            Map.of("type", "function", "function", Map.of(
                    "name", "convert_unit",
                    "description", "单位换算：把一种单位换算成另一种单位。支持长度（米/千米/公里/厘米/毫米/英尺/英寸/英里）、重量（千克/公斤/克/吨/斤/磅/盎司）、温度（摄氏度/华氏度/开尔文）、时间（秒/分钟/小时/天）、容量（升/毫升/立方米）。例：100千米等于多少米、36华氏度等于多少摄氏度",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "value", Map.of("type", "number", "description", "要换算的数值，如 100"),
                                    "from_unit", Map.of("type", "string", "description", "原单位，如：千米"),
                                    "to_unit", Map.of("type", "string", "description", "目标单位，如：米")),
                            "required", List.of("value", "from_unit", "to_unit")))));

    /** 带工具定义的对话请求 */
    private String postChat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", messages,
                "tools", tools
        ));
        return restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    /** 识别图片内容，返回文字描述 */
    public String describeImage(byte[] imageBytes, String userText) {
        // 根据图片文件头判断真实格式，避免 MIME 写错被接口拒绝
        String mime = detectImageMime(imageBytes);
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        // 视觉请求的 content 是数组：图片 + 文字提示
        List<Object> content = new ArrayList<>();
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)));
        content.add(Map.of("type", "text", "text",
                (userText == null || userText.isBlank()) ? "请描述这张图片的内容" : userText));

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", visionModel,
                "messages", List.of(Map.of("role", "user", "content", content))
        ));
        return callApi(requestBody);
    }

    /** 公共的 HTTP 调用 + 解析，chat() 和 describeImage() 共用 */
    private String callApi(String requestBody) {
        String responseBody = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    /** 看图片文件头（魔数）判断格式，微信发的图大多是 JPEG */
    private String detectImageMime(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return "image/jpeg"; // 默认按 JPEG 处理
    }


    /** 一条对话消息：role 是 user/assistant/system */
    record ChatMessage(String role, String content) {
    }


}
