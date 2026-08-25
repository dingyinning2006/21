package com.example.demo.m5.model;

import java.util.List;

/**
 * 动态重排结果。
 *
 * <p>M5-002 验收要求：规则输出可解释原因，供 M1 生成自然语言回复。
 *
 * @param adjustedTasks    调整后的当天任务列表（已排序）
 * @param reasons          调整原因列表（可解释，每条对应一个调整动作）
 * @param loadLevel        调整后的负担等级：LIGHT / NORMAL / FULL
 * @param totalMinutes     调整后任务总时长
 * @param useFallbackCount 使用缩小版本的任务数量
 */
public record RearrangementResult(
        List<TaskInstance> adjustedTasks,
        List<AdjustmentReason> reasons,
        LoadLevel loadLevel,
        int totalMinutes,
        int useFallbackCount
) {
    public enum LoadLevel {
        LIGHT("轻负担"),
        NORMAL("正常"),
        FULL("满负荷");

        private final String displayName;

        LoadLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
