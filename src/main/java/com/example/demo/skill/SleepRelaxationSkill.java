package com.example.demo.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 睡眠放松 Skill（M2-002）
 * 命中"失眠/睡不着/睡前/入睡困难"等关键词时，提供睡前放松引导
 * 支持渐进式肌肉放松和睡前呼吸两种模式
 * 参数错误或不适合场景时有明确失败结果
 */
@Component
public class SleepRelaxationSkill implements Keyword {

    private static final List<String> KEYWORDS = List.of(
            "失眠", "睡不着", "入睡", "睡前", "睡眠不好",
            "翻来覆去", "入睡困难", "睡眠质量差", "睡眠放松"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean matches(String userText) {
        if (userText == null || userText.isBlank()) {
            return false;
        }
        String lower = userText.toLowerCase();
        return KEYWORDS.stream().anyMatch(k -> lower.contains(k.toLowerCase()));
    }

    @Override
    public String executeText(String userText) {
        if (userText == null || userText.isBlank()) {
            return "请告诉我你想进行哪种睡前放松：渐进式肌肉放松 或 睡前呼吸？";
        }

        String mode = detectMode(userText);
        return generateGuide(mode);
    }

    private String detectMode(String userText) {
        if (userText.contains("肌肉") || userText.contains("放松身体") || userText.contains("渐进")) {
            return "muscle";
        }
        if (userText.contains("呼吸") || userText.contains("深呼吸")) {
            return "breathing";
        }
        // 默认推荐渐进式肌肉放松，适合睡前
        return "muscle";
    }

    private String generateGuide(String mode) {
        if ("muscle".equals(mode)) {
            return """
                    🌙 渐进式肌肉放松引导（睡前版）
                    
                    准备：躺在床上，闭上眼睛，双手放在身体两侧
                    
                    从脚趾开始，依次向上放松每个部位：
                    
                    1️⃣ 脚趾和双脚：绷紧5秒，然后放松10秒，感受脚部的沉重感
                    2️⃣ 小腿：绷紧5秒，放松10秒
                    3️⃣ 大腿：绷紧5秒，放松10秒
                    4️⃣ 臀部和腹部：绷紧5秒，放松10秒
                    5️⃣ 双手和手臂：握拳5秒，放松10秒
                    6️⃣ 肩膀：耸肩5秒，放松10秒
                    7️⃣ 面部：皱眉、咬紧牙关5秒，然后完全放松
                    
                    完成后，深呼吸3次，感受全身的放松和沉重感。
                    如果还睡不着，可以再做一轮，或者把注意力放在呼吸上。
                    
                    💡 小提示：
                    - 如果20分钟后仍无法入睡，建议起床做些安静的事（如看书），有困意再回床上
                    - 不要看时间，越看越焦虑
                    - 长期失眠（超过2周）建议咨询专业医生
                    
                    晚安，祝你有个好梦 🌙
                    """;
        }

        if ("breathing".equals(mode)) {
            return """
                    🌙 睡前呼吸放松引导
                    
                    准备：躺在床上，闭上眼睛，一只手放在腹部
                    
                    第1步：用鼻子缓慢吸气4秒，感受腹部鼓起
                    第2步：屏住呼吸4秒
                    第3步：用嘴缓慢呼气6秒，感受腹部回落
                    
                    重复8-10个循环
                    
                    随着每次呼气，想象身体的紧张和压力都在离开你。
                    你的身体越来越沉，越来越放松，越来越困...
                    
                    💡 如果思绪很多，不要强迫自己"不想"，
                    只是观察这些想法，然后把注意力轻轻拉回到呼吸上。
                    
                    晚安 🌙
                    """;
        }

        return "不支持的放松模式，请选择 渐进式肌肉放松 或 睡前呼吸。";
    }

    // ==================== Skill 接口实现（供 Function Calling 使用）====================

    @Override
    public String getName() {
        return "sleep_relaxation";
    }

    @Override
    public String getDescription() {
        return "提供睡前放松引导，支持渐进式肌肉放松和睡前呼吸，帮助入睡";
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "mode",
                Map.of(
                        "type", "string",
                        "description", "放松模式：muscle（渐进式肌肉放松）或 breathing（睡前呼吸）",
                        "enum", List.of("muscle", "breathing")
                )
        );
    }

    @Override
    public List<String> getRequiredParameters() {
        return List.of("mode");
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);

            JsonNode modeNode = args.get("mode");
            if (modeNode == null || modeNode.isNull() || modeNode.asText().isBlank()) {
                return "睡眠放松失败：必须指定 mode 参数（muscle 或 breathing）";
            }

            String mode = modeNode.asText();
            if (!"muscle".equals(mode) && !"breathing".equals(mode)) {
                return "睡眠放松失败：mode 只能是 muscle 或 breathing，当前值：" + mode;
            }

            return generateGuide(mode);
        } catch (Exception e) {
            return "睡眠放松失败：参数格式不正确 - " + e.getMessage();
        }
    }
}
