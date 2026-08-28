package com.example.demo.agent.contract.m5.model;

/**
 * 任务调整原因，可解释，供 M1 生成自然语言回复。
 *
 * @param reasonType    原因类型
 * @param description   人类可读的原因描述
 * @param affectedTaskId 受影响的任务实例ID（可选，全局调整时为null）
 * @param action        采取的调整动作
 */
public record AdjustmentReason(
        ReasonType reasonType,
        String description,
        String affectedTaskId,
        AdjustmentAction action
) {
    public enum ReasonType {
        HIGH_STRESS("压力过高"),
        POOR_SLEEP("睡眠质量差"),
        CONSECUTIVE_MISSED("连续未完成"),
        RISING_STRESS("压力呈上升趋势"),
        LOW_COMPLETION("昨日完成率低"),
        TIME_BUDGET("时间预算有限"),
        KEY_EVENT_APPROACHING("关键事件临近"),
        NORMAL_PROGRESSION("正常推进");

        private final String displayName;

        ReasonType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AdjustmentAction {
        REDUCE_TASK_COUNT("减少任务数量"),
        DOWNGRADE_DIFFICULTY("降低任务难度"),
        USE_FALLBACK_VERSION("启用缩小版本"),
        SHORTEN_DURATION("缩短任务时长"),
        REORDER_PRIORITY("调整任务优先级"),
        ADD_TASK("增加任务"),
        NO_CHANGE("保持不变");

        private final String displayName;

        AdjustmentAction(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
