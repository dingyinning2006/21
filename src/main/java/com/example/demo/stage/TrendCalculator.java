package com.example.demo.stage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.SafetyLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * M6-001：指标与趋势计算。
 * <p>
 * 输入：用户多日的 {@link CheckInRecord}（由 M4 提供）。
 * 输出：{@link IndicatorResult}，包含压力变化、睡眠变化、任务完成率、连续打卡天数、
 * 趋势判定、有效方法和风险提示。
 * <p>
 * 指标定义：
 * <ul>
 *   <li>压力变化 = 最后一天 stressLevel − 第一天 stressLevel（负数表示下降/好转）</li>
 *   <li>睡眠变化 = 最后一天 sleepHours − 第一天 sleepHours（正数表示增加/好转）</li>
 *   <li>任务完成率 = 所有天 completionRate 的算术平均</li>
 *   <li>连续打卡天数 = 从最后一天往前数，日期连续且有记录的天数</li>
 * </ul>
 * 缺失值处理：记录按日期排序后跳过日期不连续的间隔，不影响其他天的计算。
 * 重复打卡：同一天只保留最后一条（按 date 去重）。
 * 数据不足（< 2 天）时，趋势判定为 "样本不足"。
 */
@Service
public class TrendCalculator {

    private static final int MIN_DAYS_FOR_TREND = 2;

    /**
     * 计算多日打卡记录的全部指标。
     *
     * @param records     多日打卡记录（不要求排序，内部会按日期排序和去重）
     * @param safetyLevel 当前安全等级，影响报告的建议方向
     * @return 指标计算结果
     */
    public IndicatorResult calculate(List<CheckInRecord> records, SafetyLevel safetyLevel) {
        if (records == null || records.isEmpty()) {
            return IndicatorResult.empty(safetyLevel);
        }

        // 按日期排序，同一天保留最后一条
        List<CheckInRecord> sorted = deduplicateAndSort(records);

        if (sorted.size() < MIN_DAYS_FOR_TREND) {
            return IndicatorResult.insufficient(sorted.get(0), safetyLevel);
        }

        CheckInRecord first = sorted.get(0);
        CheckInRecord last = sorted.get(sorted.size() - 1);

        double stressDelta = last.stressLevel() - first.stressLevel();
        double sleepDelta = last.sleepHours() - first.sleepHours();
        double avgCompletionRate = sorted.stream()
                .mapToDouble(CheckInRecord::completionRate)
                .average()
                .orElse(0.0);
        int streakDays = calculateStreak(sorted);
        String trend = classifyTrend(stressDelta, avgCompletionRate);
        List<String> effectiveMethods = extractEffectiveMethods(sorted);
        List<String> concerns = extractConcerns(sorted, stressDelta, avgCompletionRate);

        return new IndicatorResult(
                sorted.size(),
                first.stressLevel(),
                last.stressLevel(),
                stressDelta,
                first.sleepHours(),
                last.sleepHours(),
                sleepDelta,
                avgCompletionRate,
                streakDays,
                trend,
                effectiveMethods,
                concerns,
                safetyLevel
        );
    }

    /**
     * 按日期排序，同一天只保留最后一条记录。
     */
    private List<CheckInRecord> deduplicateAndSort(List<CheckInRecord> records) {
        return records.stream()
                .sorted(Comparator.comparing(CheckInRecord::date))
                .collect(java.util.stream.Collectors.toMap(
                        CheckInRecord::date,
                        r -> r,
                        (existing, replacement) -> replacement,
                        java.util.TreeMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    /**
     * 从最后一天往前数连续打卡天数。
     * 日期间隔超过 1 天则中断。
     */
    private int calculateStreak(List<CheckInRecord> sorted) {
        int streak = 1;
        for (int i = sorted.size() - 1; i > 0; i--) {
            long gap = java.time.temporal.ChronoUnit.DAYS.between(
                    sorted.get(i - 1).date(), sorted.get(i).date());
            if (gap == 1) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * 根据压力变化和任务完成率判定趋势。
     * 能区分多种组合情况，不输出单一结论。
     */
    private String classifyTrend(double stressDelta, double avgCompletionRate) {
        boolean stressDown = stressDelta < -1;
        boolean stressFlat = Math.abs(stressDelta) <= 1;
        boolean stressUp = stressDelta > 1;
        boolean highCompletion = avgCompletionRate >= 0.6;
        boolean lowCompletion = avgCompletionRate < 0.4;

        if (stressDown && highCompletion) {
            return "明显改善";
        }
        if (stressDown && lowCompletion) {
            return "压力下降但任务完成率低";
        }
        if (stressDown) {
            return "有所改善";
        }
        if (stressFlat && highCompletion) {
            return "稳定且执行力好";
        }
        if (stressFlat && lowCompletion) {
            return "稳定但任务完成不足";
        }
        if (stressFlat) {
            return "基本稳定";
        }
        if (stressUp && highCompletion) {
            return "压力上升但执行力好";
        }
        if (stressUp) {
            return "需要关注";
        }
        return "基本稳定";
    }

    /**
     * 从已完成任务中提取高频类别作为"有效方法"。
     * 只在完成率 >= 50% 时才认为方法有效。
     */
    private List<String> extractEffectiveMethods(List<CheckInRecord> sorted) {
        long totalCompleted = sorted.stream()
                .mapToLong(r -> r.completedTaskIds().size())
                .sum();
        if (totalCompleted == 0) {
            return List.of();
        }

        // 完成率低于 50% 不认为有"有效方法"
        double avgRate = sorted.stream()
                .mapToDouble(CheckInRecord::completionRate)
                .average()
                .orElse(0);
        if (avgRate < 0.5) {
            return List.of();
        }

        // 从 note 字段提取关键词作为有效方法提示
        List<String> methods = new ArrayList<>();
        for (CheckInRecord r : sorted) {
            if (r.note() != null && !r.note().isBlank()) {
                String note = r.note().toLowerCase();
                if (note.contains("呼吸") || note.contains("放松")) {
                    if (!methods.contains("呼吸训练")) methods.add("呼吸训练");
                }
                if (note.contains("运动") || note.contains("跑步") || note.contains("散步")) {
                    if (!methods.contains("运动")) methods.add("运动");
                }
                if (note.contains("早睡") || note.contains("作息") || note.contains("睡眠")) {
                    if (!methods.contains("规律作息")) methods.add("规律作息");
                }
                if (note.contains("聊天") || note.contains("倾诉") || note.contains("朋友")) {
                    if (!methods.contains("社交支持")) methods.add("社交支持");
                }
            }
        }
        return methods.isEmpty() ? List.of("坚持打卡") : methods;
    }

    /**
     * 提取需要关注的风险点。
     */
    private List<String> extractConcerns(
            List<CheckInRecord> sorted, double stressDelta, double avgCompletionRate) {
        List<String> concerns = new ArrayList<>();

        if (stressDelta > 2) {
            concerns.add("压力明显上升（+" + String.format("%.0f", stressDelta) + "分）");
        }

        if (avgCompletionRate < 0.3) {
            concerns.add("任务完成率过低（" + String.format("%.0f", avgCompletionRate * 100) + "%）");
        }

        // 检查是否有连续 3 天压力上升
        if (sorted.size() >= 3) {
            int consecutiveUp = 0;
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).stressLevel() > sorted.get(i - 1).stressLevel()) {
                    consecutiveUp++;
                    if (consecutiveUp >= 2) {
                        concerns.add("连续多天压力上升");
                        break;
                    }
                } else {
                    consecutiveUp = 0;
                }
            }
        }

        // 检查睡眠不足
        long sleepDeficitDays = sorted.stream()
                .filter(r -> r.sleepHours() < 6)
                .count();
        if (sleepDeficitDays >= sorted.size() / 2.0) {
            concerns.add("多数天睡眠不足 6 小时");
        }

        return concerns;
    }
}
