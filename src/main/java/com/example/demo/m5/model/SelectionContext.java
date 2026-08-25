package com.example.demo.m5.model;

import java.util.List;

/**
 * 任务选择与动态重排的上下文。
 *
 * <p>M5-002 验收要求：根据剩余天数、压力、睡眠和完成度选择任务。
 * 压力升高或连续未完成时，任务数量、时长或难度会下降。
 *
 * @param dayNumber            当前是7天计划的第几天（1-7）
 * @param daysRemaining        距离关键事件（如面试/考试）的剩余天数；无明确事件时为7
 * @param stressLevel          当前压力等级 1-10（10最高）
 * @param stressLevelTrend     压力趋势：上升/稳定/下降
 * @param sleepQuality         昨晚睡眠质量 1-5（5最好）
 * @param yesterdayCompletionRate  昨天任务完成率 0.0-1.0
 * @param consecutiveMissedDays    连续未完成任务的天数
 * @param primaryStressSources 主要压力源列表，用于匹配任务类别
 * @param keyEventType         关键事件类型：INTERVIEW / EXAM / NONE
 * @param totalTaskBudgetMinutes 当天可用的总任务时间预算（分钟）
 */
public record SelectionContext(
        int dayNumber,
        int daysRemaining,
        int stressLevel,
        StressTrend stressLevelTrend,
        int sleepQuality,
        double yesterdayCompletionRate,
        int consecutiveMissedDays,
        List<String> primaryStressSources,
        KeyEventType keyEventType,
        int totalTaskBudgetMinutes
) {
    public SelectionContext {
        if (dayNumber < 1 || dayNumber > 7) {
            throw new IllegalArgumentException("dayNumber 必须在 1-7 之间");
        }
        if (daysRemaining < 0) {
            throw new IllegalArgumentException("daysRemaining 不能为负");
        }
        if (stressLevel < 1 || stressLevel > 10) {
            throw new IllegalArgumentException("stressLevel 必须在 1-10 之间");
        }
        if (sleepQuality < 1 || sleepQuality > 5) {
            throw new IllegalArgumentException("sleepQuality 必须在 1-5 之间");
        }
        if (yesterdayCompletionRate < 0.0 || yesterdayCompletionRate > 1.0) {
            throw new IllegalArgumentException("yesterdayCompletionRate 必须在 0-1 之间");
        }
        if (consecutiveMissedDays < 0) {
            throw new IllegalArgumentException("consecutiveMissedDays 不能为负");
        }
        if (totalTaskBudgetMinutes <= 0) {
            throw new IllegalArgumentException("totalTaskBudgetMinutes 必须大于0");
        }
    }

    /**
     * 压力趋势。
     */
    public enum StressTrend {
        RISING("上升"),
        STABLE("稳定"),
        FALLING("下降");

        private final String displayName;

        StressTrend(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 关键事件类型。
     */
    public enum KeyEventType {
        INTERVIEW("面试"),
        EXAM("考试"),
        NONE("无明确事件");

        private final String displayName;

        KeyEventType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 判断是否需要减负（压力高、睡眠差、连续未完成）。
     */
    public boolean needsReducedLoad() {
        return stressLevel >= 7
                || sleepQuality <= 2
                || consecutiveMissedDays >= 2
                || (stressLevelTrend == StressTrend.RISING && stressLevel >= 5);
    }

    /**
     * 获取建议的任务数量。
     * 状态好时3个，状态一般时2个，需要减负时1个。
     */
    public int getSuggestedTaskCount() {
        if (needsReducedLoad()) {
            return 1;
        }
        if (stressLevel >= 5 || sleepQuality <= 3 || yesterdayCompletionRate < 0.5) {
            return 2;
        }
        return 3;
    }
}
