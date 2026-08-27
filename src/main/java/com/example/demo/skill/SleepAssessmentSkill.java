package com.example.demo.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SleepAssessmentSkill implements Keyword {

    private static final Pattern SLEEP_HOURS_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(小时|时|h)", Pattern.CASE_INSENSITIVE);

    private static final Pattern AWAKENINGS_PATTERN =
            Pattern.compile("(\\d+)\\s*次");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean matches(String userText) {
        if (userText == null || userText.isBlank()) {
            return false;
        }

        return userText.contains("睡眠")
                || userText.contains("睡觉")
                || userText.contains("失眠")
                || userText.contains("入睡")
                || userText.contains("醒来")
                || userText.contains("作息");
    }

    @Override
    public String executeText(String userText) {
        if (userText == null || userText.isBlank()) {
            return "请告诉我你的睡眠时长和夜间醒来次数。";
        }

        Matcher sleepHoursMatcher = SLEEP_HOURS_PATTERN.matcher(userText);
        Matcher awakeningsMatcher = AWAKENINGS_PATTERN.matcher(userText);

        if (!sleepHoursMatcher.find()) {
            return "请提供每天的睡眠时长，例如：每天睡 7.5 小时。";
        }

        if (!awakeningsMatcher.find()) {
            return "请提供夜间醒来次数，例如：晚上醒来 2 次。";
        }

        try {
            double sleepHours = Double.parseDouble(sleepHoursMatcher.group(1));
            int awakenings = Integer.parseInt(awakeningsMatcher.group(1));

            AssessmentResult result = assess(sleepHours, awakenings);

            return "睡眠评估：" + result.level()
                    + "\n每天睡眠时长：" + result.sleepHours() + " 小时"
                    + "\n夜间醒来次数：" + result.awakenings() + " 次"
                    + "\n建议：" + result.advice();
        } catch (NumberFormatException e) {
            return "睡眠数据格式不正确，请重新描述。";
        } catch (IllegalArgumentException e) {
            return "睡眠评估失败：" + e.getMessage();
        }
    }

    @Override
    public String getName() {
        return "sleep_assessment";
    }

    @Override
    public String getDescription() {
        return "根据睡眠时长和夜间醒来次数评估睡眠情况";
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "sleep_hours",
                Map.of(
                        "type", "number",
                        "description", "每天平均睡眠时长，单位小时"
                ),
                "awakenings",
                Map.of(
                        "type", "integer",
                        "description", "每晚平均醒来次数"
                )
        );
    }

    @Override
    public List<String> getRequiredParameters() {
        return List.of("sleep_hours", "awakenings");
    }

    /**
     * 供 LLM Function Calling 使用。
     * 这里接收 JSON 参数，返回 JSON 结果。
     */
    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode arguments = objectMapper.readTree(argumentsJson);

            JsonNode sleepHoursNode = arguments.get("sleep_hours");
            JsonNode awakeningsNode = arguments.get("awakenings");

            if (sleepHoursNode == null
                    || !sleepHoursNode.isNumber()
                    || !Double.isFinite(sleepHoursNode.doubleValue())) {
                throw new IllegalArgumentException("sleep_hours 必须是有限数字");
            }

            if (awakeningsNode == null || !awakeningsNode.isInt()) {
                throw new IllegalArgumentException("awakenings 必须是整数");
            }

            double sleepHours = sleepHoursNode.doubleValue();
            int awakenings = awakeningsNode.intValue();

            return objectMapper.writeValueAsString(
                    assess(sleepHours, awakenings)
            );
        } catch (IllegalArgumentException e) {
            return "睡眠评估失败：" + e.getMessage();
        } catch (Exception e) {
            return "睡眠评估失败：参数格式不正确";
        }
    }

    private AssessmentResult assess(double sleepHours, int awakenings) {
        if (!Double.isFinite(sleepHours)) {
            throw new IllegalArgumentException("睡眠时长必须是有限数字");
        }

        if (sleepHours < 0 || sleepHours > 24) {
            throw new IllegalArgumentException("睡眠时长必须在 0 到 24 小时之间");
        }

        if (awakenings < 0) {
            throw new IllegalArgumentException("夜间醒来次数不能小于 0");
        }

        String level;
        String advice;

        if (sleepHours >= 7 && sleepHours <= 9 && awakenings <= 1) {
            level = "良好";
            advice = "你的睡眠时长和夜间睡眠连续性较好，请继续保持规律作息。";
        } else if (sleepHours >= 6 && awakenings <= 2) {
            level = "一般";
            advice = "可以尝试固定入睡时间，并减少睡前使用手机。";
        } else {
            level = "需要改善";
            advice = "建议关注睡眠时长和夜间醒来情况，必要时咨询专业医生。";
        }

        return new AssessmentResult(
                sleepHours,
                awakenings,
                level,
                advice
        );
    }

    private record AssessmentResult(
            double sleepHours,
            int awakenings,
            String level,
            String advice
    ) {
    }
}
