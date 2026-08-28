package com.example.demo.agent.contract.m5.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 当天具体任务实例，从 TaskTemplate 实例化而来。
 * 包含当天的执行状态和是否使用缩小版本。
 *
 * @param instanceId      实例唯一标识
 * @param templateId      来源模板ID
 * @param category        任务类别
 * @param title           任务标题
 * @param goal            任务目标
 * @param estimatedMinutes 预计时长
 * @param difficulty      难度
 * @param suggestedTime   建议时段
 * @param steps           执行步骤
 * @param completionCriteria 完成标准
 * @param useFallback     是否使用缩小版本（状态不好时）
 * @param fallbackVersion 缩小版本描述
 * @param scheduledDate   计划执行日期
 * @param dayNumber       7天计划中的第几天（1-7）
 * @param sortOrder       当天任务的排序（0开始）
 */
public record TaskInstance(
        String instanceId,
        String templateId,
        TaskCategory category,
        String title,
        String goal,
        int estimatedMinutes,
        TaskDifficulty difficulty,
        TimeOfDay suggestedTime,
        List<String> steps,
        String completionCriteria,
        boolean useFallback,
        String fallbackVersion,
        LocalDate scheduledDate,
        int dayNumber,
        int sortOrder
) {
    public TaskInstance {
        if (dayNumber < 1 || dayNumber > 7) {
            throw new IllegalArgumentException("dayNumber 必须在 1-7 之间，当前: " + dayNumber);
        }
    }

    /**
     * 获取实际应执行的时长（缩小版本时取一半）。
     */
    public int getActualMinutes() {
        return useFallback ? Math.max(5, estimatedMinutes / 2) : estimatedMinutes;
    }

    /**
     * 获取实际显示的任务描述（缩小版本时显示fallback描述）。
     */
    public String getDisplayDescription() {
        return useFallback ? fallbackVersion : goal;
    }
}
