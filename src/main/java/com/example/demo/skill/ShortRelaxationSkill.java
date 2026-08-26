package com.example.demo.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 短时放松 Skill（M2-002）
 * 命中"压力大/太累/崩溃/歇口气/5分钟放松"等关键词时，提供5分钟快速放松方案
 * 支持办公室放松、正念扫描、情绪重置三种模式
 * 参数错误或不适合场景时有明确失败结果
 */
@Component
public class ShortRelaxationSkill implements Keyword {

    private static final List<String> KEYWORDS = List.of(
            "压力大", "压力太", "压力好", "压力很", "太累", "崩溃", "歇口气", "5分钟放松",
            "快放松", "减压", "喘不过气", "绷不住", "需要放松",
            "好累", "精疲力尽", " burnout"
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
            return "请选择一种短时放松方式：办公室放松、正念扫描、情绪重置？";
        }

        String mode = detectMode(userText);
        return generateGuide(mode);
    }

    private String detectMode(String userText) {
        if (userText.contains("办公室") || userText.contains("上班") || userText.contains("工位")) {
            return "office";
        }
        if (userText.contains("正念") || userText.contains("扫描") || userText.contains("身体")) {
            return "bodyscan";
        }
        if (userText.contains("情绪") || userText.contains("重置") || userText.contains("平复")) {
            return "emotion";
        }
        // 默认推荐办公室放松，最实用
        return "office";
    }

    private String generateGuide(String mode) {
        if ("office".equals(mode)) {
            return """
                    ⏱️ 5分钟办公室快速放松
                    
                    第1分钟：站起来，伸个大大的懒腰，转动脖子和肩膀
                    第2分钟：走到窗边，看看远处的绿色或天空，让眼睛休息
                    第3分钟：做5次深呼吸（吸气4秒，呼气6秒）
                    第4分钟：喝一杯温水，感受水的温度和味道
                    第5分钟：问自己："现在最重要的一件事是什么？"，然后只做那一件
                    
                    💡 小提示：
                    - 每工作1小时，就站起来活动2-3分钟
                    - 不要连续盯着屏幕超过2小时
                    - 如果长期感到压力过大，建议调整工作节奏或寻求支持
                    
                    放松完了吗？感觉好一点了吗？
                    """;
        }

        if ("bodyscan".equals(mode)) {
            return """
                    ⏱️ 5分钟正念身体扫描
                    
                    准备：找一个安静的地方坐下或躺下，闭上眼睛
                    
                    第1分钟：把注意力放在呼吸上，感受空气进出身体
                    第2分钟：从头顶开始，慢慢扫描到额头、眼睛、脸颊、下巴，感受这些部位的紧张，然后放松
                    第3分钟：扫描脖子、肩膀、手臂、双手，让肩膀自然下沉
                    第4分钟：扫描胸部、腹部、背部，感受呼吸时腹部的起伏
                    第5分钟：扫描大腿、小腿、双脚，感受双脚与地面的接触
                    
                    完成后，深呼吸3次，慢慢睁开眼睛。
                    
                    💡 如果某个部位感到特别紧张，多停留一会儿，想象呼吸带到那个部位，让它放松。
                    
                    感觉怎么样？
                    """;
        }

        if ("emotion".equals(mode)) {
            return """
                    ⏱️ 5分钟情绪重置
                    
                    第1步（1分钟）：命名情绪
                    现在的情绪是什么？焦虑？愤怒？沮丧？疲惫？
                    给它起个名字，承认它的存在，不要评判自己。
                    
                    第2步（1分钟）：身体着陆
                    感受双脚踩在地上的感觉，手触摸身边的物体，
                    看看周围能看到的5样东西，听到的3种声音，
                    把注意力拉回到当下。
                    
                    第3步（1分钟）：深呼吸
                    做5次4-7-8呼吸：吸气4秒，屏息7秒，呼气8秒。
                    
                    第4步（1分钟）：重新评估
                    问自己：
                    - 这件事1周后还重要吗？1个月后呢？
                    - 我现在能控制的是什么？不能控制的是什么？
                    - 现在最小的一步可以做什么？
                    
                    第5步（1分钟）：自我关怀
                    对自己说一句温柔的话，就像对好朋友说的那样。
                    "你已经很努力了，没关系的。"
                    
                    感觉好一点了吗？
                    """;
        }

        return "不支持的放松模式，请选择 办公室放松、正念扫描 或 情绪重置。";
    }

    // ==================== Skill 接口实现（供 Function Calling 使用）====================

    @Override
    public String getName() {
        return "short_relaxation";
    }

    @Override
    public String getDescription() {
        return "提供5分钟快速放松方案，支持办公室放松、正念身体扫描、情绪重置三种模式";
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "mode",
                Map.of(
                        "type", "string",
                        "description", "放松模式：office（办公室放松）、bodyscan（正念身体扫描）、emotion（情绪重置）",
                        "enum", List.of("office", "bodyscan", "emotion")
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
                return "短时放松失败：必须指定 mode 参数（office、bodyscan 或 emotion）";
            }

            String mode = modeNode.asText();
            if (!"office".equals(mode) && !"bodyscan".equals(mode) && !"emotion".equals(mode)) {
                return "短时放松失败：mode 只能是 office、bodyscan 或 emotion，当前值：" + mode;
            }

            return generateGuide(mode);
        } catch (Exception e) {
            return "短时放松失败：参数格式不正确 - " + e.getMessage();
        }
    }
}
