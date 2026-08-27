package com.example.demo.agent.contract;

import java.time.LocalDate;
import java.util.List;

/** 一段时间的变化总结，供用户查看进展并决定下一阶段建议。 */
public record StageReport(
        String userId,
        LocalDate startDate,
        LocalDate endDate,
        SafetyLevel safetyLevel,
        double stressDelta, // 结束值减开始值，负数表示压力下降
        double sleepDelta, // 结束值减开始值，正数表示睡眠增加
        double completionRate, // 统计周期内任务完成率，范围 0-1
        List<String> observedChanges,
        List<String> nextStepSuggestions,
        String summary
) {

    public StageReport {
        userId = SupportContractChecks.requireNonBlank(userId, "userId");
        startDate = SupportContractChecks.requireNonNull(startDate, "startDate");
        endDate = SupportContractChecks.requireNonNull(endDate, "endDate");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate 不能早于 startDate");
        }
        safetyLevel = SupportContractChecks.requireNonNull(safetyLevel, "safetyLevel");
        if (!Double.isFinite(stressDelta)) {
            throw new IllegalArgumentException("stressDelta 必须是有限数字");
        }
        if (!Double.isFinite(sleepDelta)) {
            throw new IllegalArgumentException("sleepDelta 必须是有限数字");
        }
        completionRate = SupportContractChecks.requireDoubleRange(completionRate, 0, 1, "completionRate");
        observedChanges = SupportContractChecks.copyStrings(observedChanges, "observedChanges");
        nextStepSuggestions = SupportContractChecks.copyStrings(nextStepSuggestions, "nextStepSuggestions");
        if (nextStepSuggestions.isEmpty()) {
            throw new IllegalArgumentException("nextStepSuggestions 不能为空");
        }
        summary = SupportContractChecks.requireNonBlank(summary, "summary");
    }
}
