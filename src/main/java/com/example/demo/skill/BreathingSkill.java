package com.example.demo.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 呼吸训练 Skill（M2-002）
 * 命中"呼吸/深呼吸/放松/焦虑/紧张"等关键词时，引导用户进行呼吸训练
 * 支持 4-7-8 呼吸法和腹式呼吸两种模式
 * 参数错误或不适合场景时有明确失败结果
 */
@Component
public class BreathingSkill implements Keyword {

    private static final List<String> KEYWORDS = List.of(
            "呼吸", "深呼吸", "478", "4-7-8", "腹式呼吸",
            "放松", "焦虑", "紧张", "心慌", "心跳快"
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
            return "请告诉我你想进行哪种呼吸训练：4-7-8呼吸法 或 腹式呼吸？";
        }

        // 判断用户想要的呼吸模式
        String mode = detectMode(userText);
        int rounds = detectRounds(userText);

        return generateGuide(mode, rounds);
    }

    private String detectMode(String userText) {
        if (userText.contains("478") || userText.contains("4-7-8") || userText.contains("4 7 8")) {
            return "478";
        }
        if (userText.contains("腹式")) {
            return "belly";
        }
        // 默认推荐 4-7-8，适合快速缓解焦虑
        return "478";
    }

    private int detectRounds(String userText) {
        // 简单提取数字
        for (String word : userText.split("[^0-9]+")) {
            if (!word.isEmpty()) {
                try {
                    int n = Integer.parseInt(word);
                    if (n >= 1 && n <= 20) {
                        return n;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 4; // 默认4轮
    }

    private String generateGuide(String mode, int rounds) {
        if ("478".equals(mode)) {
            return """
                    🌬️ 4-7-8 呼吸训练引导
                    
                    准备：找一个舒适的姿势，舌尖轻抵上颚
                    
                    第1步：用鼻子安静吸气，数 4 秒
                    第2步：屏住呼吸，数 7 秒
                    第3步：用嘴缓慢呼气，数 8 秒（发出"呼"的声音）
                    
                    重复 %d 个循环
                    
                    ⚠️ 注意：
                    - 初学者可能会感到头晕，属正常现象，可减少屏息时间
                    - 不要在驾驶或操作机械时练习
                    - 有严重呼吸系统疾病请先咨询医生
                    
                    练习完感觉怎么样？可以告诉我你的感受。
                    """.formatted(rounds);
        }

        if ("belly".equals(mode)) {
            return """
                    🌬️ 腹式呼吸训练引导
                    
                    准备：坐姿或仰卧，一只手放在胸口，另一只手放在腹部
                    
                    第1步：用鼻子缓慢吸气，让腹部鼓起（腹部的手抬起，胸口的手尽量不动）
                    第2步：用嘴缓慢呼气，腹部自然回落
                    
                    吸气 4 秒，呼气 6 秒，重复 %d 个循环
                    
                    💡 效果：激活副交感神经，降低心率和血压，缓解压力反应
                    建议每天练习 2-3 次，尤其在感到压力或焦虑时
                    
                    练习完感觉怎么样？
                    """.formatted(rounds);
        }

        return "不支持的呼吸训练模式，请选择 4-7-8呼吸法 或 腹式呼吸。";
    }

    // ==================== Skill 接口实现（供 Function Calling 使用）====================

    @Override
    public String getName() {
        return "breathing_exercise";
    }

    @Override
    public String getDescription() {
        return "引导用户进行呼吸训练，支持4-7-8呼吸法和腹式呼吸，用于缓解焦虑和紧张";
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "mode",
                Map.of(
                        "type", "string",
                        "description", "呼吸训练模式：478（4-7-8呼吸法）或 belly（腹式呼吸）",
                        "enum", List.of("478", "belly")
                ),
                "rounds",
                Map.of(
                        "type", "integer",
                        "description", "重复循环次数，1-20之间，默认4"
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
                return "呼吸训练失败：必须指定 mode 参数（478 或 belly）";
            }

            String mode = modeNode.asText();
            if (!"478".equals(mode) && !"belly".equals(mode)) {
                return "呼吸训练失败：mode 只能是 478 或 belly，当前值：" + mode;
            }

            int rounds = 4;
            JsonNode roundsNode = args.get("rounds");
            if (roundsNode != null && roundsNode.isInt()) {
                rounds = roundsNode.asInt();
                if (rounds < 1 || rounds > 20) {
                    return "呼吸训练失败：rounds 必须在 1-20 之间，当前值：" + rounds;
                }
            }

            return generateGuide(mode, rounds);
        } catch (Exception e) {
            return "呼吸训练失败：参数格式不正确 - " + e.getMessage();
        }
    }
}
