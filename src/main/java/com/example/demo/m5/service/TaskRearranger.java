package com.example.demo.m5.service;

import com.example.demo.m5.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务动态重排器。
 *
 * <p>M5-002 核心组件之二：根据前一天的打卡反馈，动态调整第二天的任务。
 * 验收标准：
 * - 压力升高或连续未完成时，任务数量、时长或难度会下降
 * - 规则输出可解释原因，供 M1 生成自然语言回复
 * - 不会机械追加任务
 */
public class TaskRearranger {

    private final TaskSelector taskSelector;

    public TaskRearranger(TaskSelector taskSelector) {
        this.taskSelector = taskSelector;
    }

    /**
     * 根据前一天的打卡记录，重排第二天的任务。
     *
     * @param yesterdayContext  昨天的选择上下文（用于对比）
     * @param todayContext      今天的选择上下文（含更新后的压力、睡眠、完成度）
     * @param yesterdayTasks    昨天的任务列表
     * @param yesterdayCompleted 昨天完成的任务ID列表
     * @param planStartDate     计划开始日期
     * @return 重排结果，含调整后的任务和可解释原因
     */
    public RearrangementResult rearrange(
            SelectionContext yesterdayContext,
            SelectionContext todayContext,
            List<TaskInstance> yesterdayTasks,
            List<String> yesterdayCompleted,
            LocalDate planStartDate) {

        List<AdjustmentReason> reasons = new ArrayList<>();

        // 1. 分析变化，生成调整原因
        analyzeChanges(yesterdayContext, todayContext, yesterdayTasks, yesterdayCompleted, reasons);

        // 2. 用新的上下文重新选择任务
        List<TaskInstance> adjustedTasks = taskSelector.selectForDay(todayContext, planStartDate);

        // 3. 如果有未完成的重要任务，且今天状态允许，顺延到今天（但不增加总数量）
        carryOverUnfinished(yesterdayTasks, yesterdayCompleted, adjustedTasks, todayContext, reasons);

        // 4. 计算汇总信息
        int totalMinutes = adjustedTasks.stream().mapToInt(TaskInstance::getActualMinutes).sum();
        int fallbackCount = (int) adjustedTasks.stream().filter(TaskInstance::useFallback).count();
        RearrangementResult.LoadLevel loadLevel = determineLoadLevel(todayContext, adjustedTasks.size());

        // 5. 如果没有任何调整原因，添加一个正常推进的原因
        if (reasons.isEmpty()) {
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.NORMAL_PROGRESSION,
                    "状态稳定，按原计划推进",
                    null,
                    AdjustmentReason.AdjustmentAction.NO_CHANGE
            ));
        }

        return new RearrangementResult(adjustedTasks, reasons, loadLevel, totalMinutes, fallbackCount);
    }

    /**
     * 分析从昨天到今天的变化，生成调整原因。
     */
    private void analyzeChanges(
            SelectionContext yesterday,
            SelectionContext today,
            List<TaskInstance> yesterdayTasks,
            List<String> yesterdayCompleted,
            List<AdjustmentReason> reasons) {

        // 压力变化
        if (today.stressLevel() > yesterday.stressLevel()) {
            int increase = today.stressLevel() - yesterday.stressLevel();
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.RISING_STRESS,
                    String.format("压力等级从%d上升到%d（上升%d），减轻今天的任务负担",
                            yesterday.stressLevel(), today.stressLevel(), increase),
                    null,
                    AdjustmentReason.AdjustmentAction.REDUCE_TASK_COUNT
            ));
        }

        // 绝对压力过高
        if (today.stressLevel() >= 7) {
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.HIGH_STRESS,
                    String.format("当前压力等级%d（较高），任务难度上限下调，优先保证休息", today.stressLevel()),
                    null,
                    AdjustmentReason.AdjustmentAction.DOWNGRADE_DIFFICULTY
            ));
        }

        // 睡眠质量差
        if (today.sleepQuality() <= 2) {
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.POOR_SLEEP,
                    String.format("昨晚睡眠质量%d/5（较差），今天减少任务量，优先恢复精力", today.sleepQuality()),
                    null,
                    AdjustmentReason.AdjustmentAction.REDUCE_TASK_COUNT
            ));
        }

        // 连续未完成
        if (today.consecutiveMissedDays() >= 2) {
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.CONSECUTIVE_MISSED,
                    String.format("已连续%d天未完成任务，今天启用缩小版本并减少数量，先重建完成感",
                            today.consecutiveMissedDays()),
                    null,
                    AdjustmentReason.AdjustmentAction.USE_FALLBACK_VERSION
            ));
        }

        // 昨天完成率低
        if (today.yesterdayCompletionRate() < 0.5 && !yesterdayTasks.isEmpty()) {
            int completed = yesterdayCompleted.size();
            int total = yesterdayTasks.size();
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.LOW_COMPLETION,
                    String.format("昨天完成率%.0f%%（%d/%d），今天适当减少任务量，确保能完成",
                            today.yesterdayCompletionRate() * 100, completed, total),
                    null,
                    AdjustmentReason.AdjustmentAction.REDUCE_TASK_COUNT
            ));
        }

        // 关键事件临近
        if (today.daysRemaining() <= 2 && today.keyEventType() != SelectionContext.KeyEventType.NONE) {
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.KEY_EVENT_APPROACHING,
                    String.format("%s还有%d天，进入冲刺阶段，任务聚焦核心准备",
                            today.keyEventType().getDisplayName(), today.daysRemaining()),
                    null,
                    AdjustmentReason.AdjustmentAction.REORDER_PRIORITY
            ));
        }
    }

    /**
     * 顺延昨天未完成的重要任务到今天。
     * 规则：只顺延1个最重要的未完成任务，且不增加今天的总任务数（替换最后一个）。
     */
    private void carryOverUnfinished(
            List<TaskInstance> yesterdayTasks,
            List<String> yesterdayCompleted,
            List<TaskInstance> todayTasks,
            SelectionContext todayContext,
            List<AdjustmentReason> reasons) {

        if (yesterdayTasks == null || yesterdayTasks.isEmpty() || todayTasks.isEmpty()) {
            return;
        }

        // 找出昨天未完成的任务
        List<TaskInstance> unfinished = yesterdayTasks.stream()
                .filter(t -> !yesterdayCompleted.contains(t.instanceId()))
                .filter(t -> t.category() != TaskCategory.BEDTIME_RELAXATION) // 睡前任务不顺延
                .toList();

        if (unfinished.isEmpty()) {
            return;
        }

        // 只在状态不是"需要减负"时才顺延，避免增加负担
        if (todayContext.needsReducedLoad()) {
            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.CONSECUTIVE_MISSED,
                    String.format("昨天有%d个任务未完成，但当前状态需要减负，暂不顺延，先专注今天的小任务",
                            unfinished.size()),
                    null,
                    AdjustmentReason.AdjustmentAction.NO_CHANGE
            ));
            return;
        }

        // 取最重要的一个（排序最靠前的）顺延，替换今天的最后一个非睡前任务
        TaskInstance toCarry = unfinished.get(0);

        // 找到今天最后一个非睡前任务的位置
        int replaceIndex = -1;
        for (int i = todayTasks.size() - 1; i >= 0; i--) {
            if (todayTasks.get(i).category() != TaskCategory.BEDTIME_RELAXATION) {
                replaceIndex = i;
                break;
            }
        }

        if (replaceIndex >= 0) {
            TaskInstance carried = new TaskInstance(
                    toCarry.templateId() + "_" + LocalDate.now(),
                    toCarry.templateId(),
                    toCarry.category(),
                    toCarry.title() + "（顺延）",
                    toCarry.goal(),
                    toCarry.estimatedMinutes(),
                    toCarry.difficulty(),
                    toCarry.suggestedTime(),
                    toCarry.steps(),
                    toCarry.completionCriteria(),
                    todayContext.yesterdayCompletionRate() < 0.5, // 昨天完成率低就用缩小版
                    toCarry.fallbackVersion(),
                    todayTasks.get(replaceIndex).scheduledDate(),
                    todayContext.dayNumber(),
                    replaceIndex
            );

            todayTasks.set(replaceIndex, carried);

            reasons.add(new AdjustmentReason(
                    AdjustmentReason.ReasonType.LOW_COMPLETION,
                    String.format("昨天未完成的任务「%s」顺延到今天，替换了一个优先级较低的任务",
                            toCarry.title()),
                    toCarry.instanceId(),
                    AdjustmentReason.AdjustmentAction.REORDER_PRIORITY
            ));
        }
    }

    private RearrangementResult.LoadLevel determineLoadLevel(SelectionContext context, int taskCount) {
        if (context.needsReducedLoad() || taskCount <= 1) {
            return RearrangementResult.LoadLevel.LIGHT;
        }
        if (taskCount <= 2) {
            return RearrangementResult.LoadLevel.NORMAL;
        }
        return RearrangementResult.LoadLevel.FULL;
    }
}
