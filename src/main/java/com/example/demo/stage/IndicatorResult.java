package com.example.demo.stage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.SafetyLevel;

import java.util.List;

/**
 * M6-001 输出：趋势计算的完整指标结果。
 * <p>
 * 不直接暴露给用户，而是交给 {@link ReportGenerator} 转化为可读报告。
 */
public record IndicatorResult(
        int totalDays,
        int firstStressLevel,
        int lastStressLevel,
        double stressDelta,
        double firstSleepHours,
        double lastSleepHours,
        double sleepDelta,
        double avgCompletionRate,
        int streakDays,
        String trend,
        List<String> effectiveMethods,
        List<String> concerns,
        SafetyLevel safetyLevel
) {

    /**
     * 完全无数据时的空结果。
     */
    static IndicatorResult empty(SafetyLevel safetyLevel) {
        return new IndicatorResult(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                "暂无数据", List.of(), List.of(), safetyLevel
        );
    }

    /**
     * 只有 1 天数据时的不完整结果。
     */
    static IndicatorResult insufficient(CheckInRecord single, SafetyLevel safetyLevel) {
        return new IndicatorResult(
                1,
                single.stressLevel(), single.stressLevel(), 0,
                single.sleepHours(), single.sleepHours(), 0,
                single.completionRate(), 1,
                "样本不足", List.of(), List.of(), safetyLevel
        );
    }

    /**
     * 是否有足够的数据生成有意义的报告（至少 2 天）。
     */
    public boolean hasEnoughData() {
        return totalDays >= 2;
    }
}
