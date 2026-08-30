package com.example.demo.stage;

import com.example.demo.agent.contract.SafetyLevel;
import com.example.demo.agent.contract.StageReport;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * M6-002：阶段报告生成。
 * <p>
 * 把 {@link IndicatorResult} 转化为用户可读的文字报告（{@link StageReport}）。
 * <p>
 * 约束：
 * <ul>
 *   <li>不使用疾病诊断语言（"焦虑症""抑郁症"等）</li>
 *   <li>不把短期数据当成医学结论</li>
 *   <li>安全状态为 URGENT 时不给普通任务建议</li>
 *   <li>文字简洁，可直接发微信或被 TTS 朗读</li>
 * </ul>
 */
@Service
public class ReportGenerator {

    /**
     * 从指标结果生成完整的阶段报告。
     *
     * @param result   趋势计算结果
     * @param startDate 报告起始日期
     * @param endDate   报告结束日期
     * @param userId    用户 ID
     * @return 可展示的阶段报告
     */
    public StageReport generate(
            IndicatorResult result, LocalDate startDate, LocalDate endDate, String userId) {

        // 空数据时直接返回固定报告，避免触发 StageReport 的校验异常
        if (result.totalDays() == 0) {
            return new StageReport(
                    userId, startDate, endDate, result.safetyLevel(),
                    0, 0, 0,
                    List.of("暂无打卡数据"),
                    List.of("请先完成每日打卡，系统会自动为你生成变化报告"),
                    "暂无打卡数据，无法生成变化报告。请先完成每日打卡。"
            );
        }

        List<String> observedChanges = buildObservedChanges(result);
        List<String> nextSteps = buildNextStepSuggestions(result);
        String summary = buildSummary(result);

        return new StageReport(
                userId,
                startDate,
                endDate,
                result.safetyLevel(),
                result.stressDelta(),
                result.sleepDelta(),
                result.avgCompletionRate(),
                observedChanges,
                nextSteps,
                summary
        );
    }

    /**
     * 生成纯文本格式的报告，可直接发微信或被 TTS 朗读。
     */
    public String generatePlainText(IndicatorResult result, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 你的 ").append(result.totalDays()).append(" 天压力调适报告\n\n");

        // 压力变化
        sb.append("📊 压力变化：");
        sb.append(result.firstStressLevel()).append("分 → ").append(result.lastStressLevel()).append("分");
        sb.append("（").append(formatDelta(result.stressDelta())).append("分，");
        sb.append(describeStressChange(result.stressDelta())).append("）\n");

        // 睡眠变化
        sb.append("😴 睡眠情况：");
        sb.append(String.format("%.1f", result.firstSleepHours())).append("小时 → ");
        sb.append(String.format("%.1f", result.lastSleepHours())).append("小时");
        sb.append("（").append(formatDelta(result.sleepDelta())).append("小时）\n");

        // 任务完成率
        sb.append("✅ 任务完成率：");
        sb.append(String.format("%.0f", result.avgCompletionRate() * 100)).append("%\n");

        // 连续打卡
        sb.append("🔥 连续打卡：").append(result.streakDays()).append(" 天\n");

        // 趋势判定
        sb.append("\n📈 整体趋势：").append(result.trend()).append("\n");

        // 有效方法
        if (!result.effectiveMethods().isEmpty()) {
            sb.append("💡 有效方法：");
            sb.append(String.join("、", result.effectiveMethods()));
            sb.append("\n");
        }

        // 风险提示
        if (!result.concerns().isEmpty()) {
            sb.append("⚠️ 需要关注：");
            sb.append(String.join("；", result.concerns()));
            sb.append("\n");
        }

        // 下一步建议
        List<String> suggestions = buildNextStepSuggestions(result);
        if (!suggestions.isEmpty()) {
            sb.append("\n🎯 下一步建议：\n");
            for (int i = 0; i < suggestions.size(); i++) {
                sb.append("- ").append(suggestions.get(i)).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private List<String> buildObservedChanges(IndicatorResult result) {
        List<String> changes = new ArrayList<>();

        if (result.stressDelta() < -1) {
            changes.add("压力从 " + result.firstStressLevel() + " 分降至 "
                    + result.lastStressLevel() + " 分，有明显改善");
        } else if (result.stressDelta() > 1) {
            changes.add("压力从 " + result.firstStressLevel() + " 分升至 "
                    + result.lastStressLevel() + " 分，需要关注");
        } else {
            changes.add("压力水平基本稳定（" + result.firstStressLevel()
                    + " → " + result.lastStressLevel() + " 分）");
        }

        if (result.sleepDelta() > 0.5) {
            changes.add("睡眠时长有所增加（+"
                    + String.format("%.1f", result.sleepDelta()) + " 小时）");
        } else if (result.sleepDelta() < -0.5) {
            changes.add("睡眠时长有所减少（"
                    + String.format("%.1f", result.sleepDelta()) + " 小时）");
        }

        if (result.avgCompletionRate() >= 0.6) {
            changes.add("任务完成率较好（"
                    + String.format("%.0f", result.avgCompletionRate() * 100) + "%）");
        } else if (result.avgCompletionRate() < 0.4) {
            changes.add("任务完成率偏低（"
                    + String.format("%.0f", result.avgCompletionRate() * 100) + "%）");
        }

        return changes;
    }

    private List<String> buildNextStepSuggestions(IndicatorResult result) {
        // 高危状态不给普通任务建议
        if (result.safetyLevel() == SafetyLevel.URGENT) {
            return List.of("请优先关注自身安全，联系信任的人或拨打热线");
        }

        List<String> suggestions = new ArrayList<>();

        if (result.stressDelta() > 1) {
            suggestions.add("压力呈上升趋势，建议适当减少任务量，优先保证休息");
        } else if (result.stressDelta() < -1) {
            suggestions.add("压力有所下降，可以继续保持当前的调适方法");
        }

        if (result.avgCompletionRate() < 0.4) {
            suggestions.add("任务完成率较低，建议把大任务拆成更小的步骤");
        } else if (result.avgCompletionRate() >= 0.7) {
            suggestions.add("执行力很好，可以适当增加挑战性任务");
        }

        if (result.streakDays() >= 5) {
            suggestions.add("连续打卡 " + result.streakDays() + " 天，坚持得很好");
        }

        if (result.concerns().stream().anyMatch(c -> c.contains("睡眠"))) {
            suggestions.add("建议关注睡眠，尝试固定入睡时间和睡前放松练习");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("继续保持当前的节奏，有任何不适可以随时告诉我");
        }

        return suggestions;
    }

    private String buildSummary(IndicatorResult result) {
        if (!result.hasEnoughData()) {
            return "数据不足 " + result.totalDays() + " 天，暂无法给出完整评估，仅供参考。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(result.totalDays()).append(" 天调适记录：");

        // 压力描述
        if (result.stressDelta() < -1) {
            sb.append("压力有所下降");
        } else if (result.stressDelta() > 1) {
            sb.append("压力有所上升");
        } else {
            sb.append("压力基本稳定");
        }

        sb.append("，任务完成率 ")
                .append(String.format("%.0f", result.avgCompletionRate() * 100))
                .append("%");

        if (!result.concerns().isEmpty()) {
            sb.append("，有 ").append(result.concerns().size()).append(" 项需要关注");
        }

        sb.append("。");
        return sb.toString();
    }

    private String formatDelta(double delta) {
        return delta >= 0 ? "+" + String.format("%.1f", delta) : String.format("%.1f", delta);
    }

    private String describeStressChange(double delta) {
        if (delta < -2) return "明显改善";
        if (delta < -0.5) return "有所改善";
        if (delta <= 0.5) return "基本持平";
        if (delta <= 2) return "略有上升";
        return "明显上升";
    }
}
