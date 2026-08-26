package com.example.demo.agent.contract;

import java.time.LocalDate;
import java.util.List;

/** 每日文字或语音打卡统一落成的记录。 */
public record CheckInRecord(
        String userId,
        LocalDate date,
        int stressLevel,
        double sleepHours, // 睡眠时长，范围 0-24 小时
        int moodLevel, // 主观心情，范围 0-10
        double completionRate, // 当日任务完成率，范围 0-1
        List<String> completedTaskIds,
        List<String> missedTaskIds,
        String note,
        SupportPhase phase
) {

    public CheckInRecord {
        userId = SupportContractChecks.requireNonBlank(userId, "userId");
        date = SupportContractChecks.requireNonNull(date, "date");
        stressLevel = SupportContractChecks.requireIntRange(stressLevel, 0, 10, "stressLevel");
        sleepHours = SupportContractChecks.requireDoubleRange(sleepHours, 0, 24, "sleepHours");
        moodLevel = SupportContractChecks.requireIntRange(moodLevel, 0, 10, "moodLevel");
        completionRate = SupportContractChecks.requireDoubleRange(completionRate, 0, 1, "completionRate");
        completedTaskIds = SupportContractChecks.copyStrings(completedTaskIds, "completedTaskIds");
        missedTaskIds = SupportContractChecks.copyStrings(missedTaskIds, "missedTaskIds");
        note = SupportContractChecks.normalizeOptional(note);
        phase = SupportContractChecks.requireNonNull(phase, "phase");
    }
}
