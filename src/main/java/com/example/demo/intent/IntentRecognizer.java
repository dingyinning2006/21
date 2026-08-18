package com.example.demo.intent;

import java.util.Set;

/**
 * 意图识别器：判断用户输入属于哪种意图
 * 目前支持：WEATHER（天气）、HELP（帮助）、CHAT（闲聊）
 */
public class IntentRecognizer {

    // 天气相关关键词（不只是"天气"）
    private static final Set<String> WEATHER_KEYWORDS = Set.of(
            "天气", "气温", "温度", "多少度", "几度",
            "下雨", "雨天", "晴天", "阴天", "多云",
            "刮风", "大风", "台风", "下雪", "雪天",
            "冷不冷", "热不热", "穿什么", "带伞", "防晒",
            "紫外线", "湿度", "预报", "天气预报"
    );

    // 帮助相关关键词
    private static final Set<String> HELP_KEYWORDS = Set.of(
            "帮助", "功能", "你是谁", "你能做什么", "你会什么",
            "怎么用", "使用说明", "help", "菜单", "指令"
    );
    // 用户要求语音回复的关键词
    private static final Set<String> VOICE_REPLY_KEYWORDS = Set.of(
            "用语音", "语音回复", "语音说", "语音讲", "念出来",
            "读出来", "说出来", "语音回答", "用声音", "语音模式"
    );

    // 用户要求文字回复的关键词
    private static final Set<String> TEXT_REPLY_KEYWORDS = Set.of(
            "文字回复", "用文字", "打字回复", "文字回答", "文字模式",
            "别发语音", "不要语音", "改成文字", "切换文字"
    );
    // 已知城市列表（用于从句子中提取城市）
    private static final Set<String> KNOWN_CITIES = Set.of(
            "北京", "上海", "广州", "深圳", "杭州", "南京",
            "成都", "武汉", "长沙", "重庆", "天津", "西安",
            "苏州", "青岛", "大连", "厦门", "宁波", "合肥",
            "福州", "济南", "郑州", "沈阳", "哈尔滨", "长春",
            "石家庄", "太原", "南昌", "南宁", "昆明", "贵阳"
    );

    public enum IntentType {
        WEATHER, HELP, CHAT,
        VOICE_REPLY,   // 新增：用户要求语音回复
        TEXT_REPLY     // 新增：用户要求文字回复,
    }

    /**
     * 识别意图
     * @return 意图类型
     */
    public IntentType recognize(String text) {
        if (text == null || text.isBlank()) {
            return IntentType.CHAT;
        }
        String input = text.toLowerCase();

        // 1. 先判断帮助
        for (String keyword : HELP_KEYWORDS) {
            if (input.contains(keyword.toLowerCase())) {
                return IntentType.HELP;
            }
        }
        // 新增：先判断回复格式控制指令（优先级最高）
        for (String keyword : VOICE_REPLY_KEYWORDS) {
            if (input.contains(keyword.toLowerCase())) {
                return IntentType.VOICE_REPLY;
            }
        }
        for (String keyword : TEXT_REPLY_KEYWORDS) {
            if (input.contains(keyword.toLowerCase())) {
                return IntentType.TEXT_REPLY;
            }
        }

        // 2. 再判断天气
        for (String keyword : WEATHER_KEYWORDS) {
            if (input.contains(keyword.toLowerCase())) {
                return IntentType.WEATHER;
            }
        }

        // 3. 兜底为闲聊
        return IntentType.CHAT;
    }

    /**
     * 从文本中提取城市名，提取不到返回 null
     */
    public String extractCity(String text) {
        if (text == null) return null;
        for (String city : KNOWN_CITIES) {
            if (text.contains(city)) {
                return city;
            }
        }
        return null;
    }
}
