package com.example.demo.agent.screening;

import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.SupportPhase;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 负责初筛阶段的对话编排。
 *
 * 当前只处理：
 * 1. 接收用户的压力描述
 * 2. 判断还缺少哪些初筛信息
 * 3. 组织下一轮追问
 */
@Service
public class ScreeningOrchestrator {
    private final Map<String, ScreeningDraft> drafts = new ConcurrentHashMap<>();
    public String handleFirstMessage(
            String userId,
            String displayName,
            String userMessage
    ) {
        if (userMessage == null || userMessage.isBlank()) {
            return "我还没有收到你的具体描述，可以告诉我最近什么事情让你感到压力吗？";
        }

        ScreeningDraft draft = updateDraft(userId, displayName, userMessage);
        if (isDraftComplete(draft)) {
            ScreeningResult result = buildScreeningResult(
                    userId,
                    draft.stressLevel(),
                    draft.sleepAffected(),
                    draft.functionImpaired(),
                    draft.stressSources()
            );

            return "初筛已经完成。"
                    + "你的压力大约是 " + result.stressLevel() + " 分。"
                    + "主要压力来源是 " + result.mainStressor() + "。"
                    + "接下来可以进入计划制定阶段。";
        }
        List<String> sources = draft.stressSources();

        String sourceText;
        if (sources.isEmpty()) {
            sourceText = "最近主要是哪方面的事情让你感到压力？";
        } else {
            sourceText = "我注意到你提到了："
                    + String.join("、", sources)
                    + "。";
        }

        Integer stressLevel = draft.stressLevel();

        if (stressLevel == null) {
            return "听起来你最近承受了不少压力。"
                    + sourceText
                    + "请用 0-10 分告诉我，现在的压力大约是几分？";
        }

        Boolean sleepAffected = draft.sleepAffected();

        if (sleepAffected == null) {
            return "收到，你现在的压力大约是 " + stressLevel + " 分。"
                    + "最近睡眠有没有受到影响？";
        }

        String sleepText = sleepAffected
                ? "我记下了，压力已经影响到你的睡眠。"
                : "我记下了，目前睡眠暂未受到明显影响。";
        Boolean functionImpaired = draft.functionImpaired();

        if (functionImpaired == null) {
            return "收到，你现在的压力大约是 " + stressLevel + " 分。"
                    + sleepText
                    + "这种压力有没有影响学习、求职或日常生活？";
        }

        String functionText = functionImpaired
                ? "我记下了，这些压力已经影响到你的学习、求职或日常生活。"
                : "我记下了，目前还没有明显影响你的学习、求职或日常生活。";

        return "收到，你现在的压力大约是 " + stressLevel + " 分。"
                + sleepText
                + functionText;
    }
    private List<String> detectStressSources(String message) {
        List<String> sources = new ArrayList<>();

        if (containsAny(message, "毕业", "论文", "答辩")) {
            sources.add("毕业压力");
        }

        if (containsAny(message, "求职", "面试", "工作")) {
            sources.add("求职压力");
        }

        if (containsAny(message, "考试", "复习", "成绩")) {
            sources.add("考试压力");
        }

        if (containsAny(message, "失眠", "睡不着", "睡眠")) {
            sources.add("睡眠问题");
        }

        if (containsAny(message, "室友", "家人", "同学", "朋友")) {
            sources.add("人际压力");
        }

        return sources;
    }
    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
    private static final Pattern STRESS_LEVEL_PATTERN =
            Pattern.compile("(?:压力|焦虑程度|焦虑)[^0-9]{0,8}(10|[0-9])(?:\\s*分)?");
    private Integer extractStressLevel(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = STRESS_LEVEL_PATTERN.matcher(message);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }
    private Boolean extractSleepAffected(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        // 先判断明确的否定表达，避免“睡眠没有受到影响”被误判为 true。
        if (containsAny(
                message,
                "没有影响睡眠",
                "睡眠没有受到影响",
                "睡得还行",
                "睡眠正常"
        )) {
            return false;
        }

        if (containsAny(
                message,
                "失眠",
                "睡不着",
                "睡不好",
                "难入睡",
                "夜里醒",
                "睡眠受到影响"
        )) {
            return true;
        }

        return null;
    }
    private Boolean extractFunctionImpaired(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        // 先识别明确的否定回答。
        if (containsAny(
                message,
                "没有影响学习",
                "没有影响工作",
                "没有影响生活",
                "还能正常学习",
                "还能正常生活"
        )) {
            return false;
        }

        if (containsAny(
                message,
                "影响学习",
                "影响求职",
                "影响工作",
                "影响日常生活",
                "无法集中注意力",
                "学不进去",
                "什么都不想做",
                "无法正常生活"
        )) {
            return true;
        }

        return null;
    }
    public ScreeningResult buildScreeningResult(
            String userId,
            int stressLevel,
            boolean sleepAffected,
            boolean functionImpaired,
            List<String> stressSources
    ) {
        String mainStressor = stressSources.isEmpty()
                ? "未明确"
                : String.join("、", stressSources);

        return new ScreeningResult(
                userId,
                LocalDate.now(),
                stressLevel,
                sleepAffected,
                functionImpaired,
                stressSources,
                mainStressor,
                SupportPhase.PLANNING
        );
    }
    private ScreeningDraft updateDraft(
            String userId,
            String displayName,
            String message
    ) {

        ScreeningDraft previous = drafts.get(userId);

        List<String> sources = new ArrayList<>();

        if (previous != null) {
            sources.addAll(previous.stressSources());
        }

        for (String source : detectStressSources(message)) {
            if (!sources.contains(source)) {
                sources.add(source);
            }
        }

        Integer stressLevel = extractStressLevel(message);
        if (stressLevel == null && previous != null) {
            stressLevel = previous.stressLevel();
        }

        Boolean sleepAffected = extractSleepAffected(message);
        if (sleepAffected == null && previous != null) {
            sleepAffected = previous.sleepAffected();
        }

        Boolean functionImpaired = extractFunctionImpaired(message);
        if (functionImpaired == null && previous != null) {
            functionImpaired = previous.functionImpaired();
        }

        ScreeningDraft updated = new ScreeningDraft(
                userId,
                displayName,
                sources,
                stressLevel,
                sleepAffected,
                functionImpaired
        );

        drafts.put(userId, updated);
        return updated;
    }
    private boolean isDraftComplete(ScreeningDraft draft) {
        return draft != null
                && !draft.stressSources().isEmpty()
                && draft.stressLevel() != null
                && draft.sleepAffected() != null
                && draft.functionImpaired() != null;
    }
}
