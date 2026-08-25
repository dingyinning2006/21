package com.example.demo.m5.service;

import com.example.demo.m5.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 任务选择器。
 *
 * <p>M5-002 核心组件之一：根据剩余天数、压力、睡眠和完成度选择当天任务。
 * 验收标准：
 * - 面试剩余7天时能生成有顺序的准备任务
 * - 压力升高或连续未完成时，任务数量、时长或难度会下降
 * - 规则输出可解释原因
 */
public class TaskSelector {

    private final TaskTemplateRepository repository;

    public TaskSelector(TaskTemplateRepository repository) {
        this.repository = repository;
    }

    /**
     * 为当天选择任务列表。
     *
     * @param context 选择上下文
     * @param planStartDate 7天计划的开始日期
     * @return 当天的任务实例列表（已排序）
     */
    public List<TaskInstance> selectForDay(SelectionContext context, LocalDate planStartDate) {
        List<TaskInstance> result = new ArrayList<>();
        int taskCount = context.getSuggestedTaskCount();
        int maxDifficulty = determineMaxDifficulty(context);
        LocalDate scheduledDate = planStartDate.plusDays(context.dayNumber() - 1);

        // 1. 选择现实任务（根据关键事件类型和压力源）
        List<TaskTemplate> realityTasks = selectRealityTasks(context, maxDifficulty, taskCount);

        // 2. 实例化现实任务
        int sortOrder = 0;
        for (TaskTemplate template : realityTasks) {
            boolean useFallback = shouldUseFallback(context, template);
            result.add(instantiate(template, scheduledDate, context.dayNumber(), sortOrder++, useFallback));
        }

        // 3. 每天必须包含一个睡前放松任务（如果预算允许）
        TaskTemplate bedtimeTask = selectBedtimeTask(context, maxDifficulty);
        if (bedtimeTask != null && hasBudgetFor(result, bedtimeTask, context)) {
            boolean useFallback = shouldUseFallback(context, bedtimeTask);
            result.add(instantiate(bedtimeTask, scheduledDate, context.dayNumber(), sortOrder++, useFallback));
        }

        return result;
    }

    /**
     * 根据关键事件类型选择现实任务。
     * 面试场景：按剩余天数倒排，生成有顺序的准备任务。
     */
    private List<TaskTemplate> selectRealityTasks(SelectionContext context, int maxDifficulty, int taskCount) {
        List<TaskTemplate> candidates;

        if (context.keyEventType() == SelectionContext.KeyEventType.INTERVIEW) {
            candidates = selectInterviewTasksByDaysRemaining(context, maxDifficulty);
        } else if (context.keyEventType() == SelectionContext.KeyEventType.EXAM) {
            candidates = selectExamTasksByDaysRemaining(context, maxDifficulty);
        } else {
            // 无明确关键事件，按压力源匹配
            candidates = selectByStressSources(context, maxDifficulty);
        }

        // 限制数量
        if (candidates.size() > taskCount) {
            candidates = candidates.subList(0, taskCount);
        }

        return candidates;
    }

    /**
     * 面试场景：根据剩余天数选择有顺序的准备任务。
     *
     * 剩余7-6天：简历梳理 + 公司研究
     * 剩余5-4天：自我介绍 + 常见问题
     * 剩余3-2天：模拟面试 + 错题/查漏
     * 剩余1天：轻量复习
     */
    private List<TaskTemplate> selectInterviewTasksByDaysRemaining(SelectionContext context, int maxDifficulty) {
        List<TaskTemplate> all = repository.findByCategoryAndMaxDifficulty(TaskCategory.INTERVIEW_PREP, maxDifficulty);
        List<TaskTemplate> ordered = new ArrayList<>();
        int days = context.daysRemaining();

        if (days >= 6) {
            // 早期：基础准备
            addIfPresent(ordered, all, "INTERVIEW_001"); // 简历亮点
            addIfPresent(ordered, all, "INTERVIEW_004"); // 公司研究
        } else if (days >= 4) {
            // 中期：内容准备
            addIfPresent(ordered, all, "INTERVIEW_002"); // 自我介绍
            addIfPresent(ordered, all, "INTERVIEW_003"); // 常见问题
        } else if (days >= 2) {
            // 后期：实战演练
            addIfPresent(ordered, all, "INTERVIEW_005"); // 模拟面试
            addIfPresent(ordered, all, "INTERVIEW_003"); // 常见问题（巩固）
        } else {
            // 前一天：轻量复习
            addIfPresent(ordered, all, "INTERVIEW_006"); // 轻量复习
        }

        // 如果按天数选出来为空（可能难度过滤掉了），取前两个
        if (ordered.isEmpty()) {
            ordered.addAll(all.stream().limit(2).toList());
        }

        return ordered;
    }

    /**
     * 考试场景：根据剩余天数选择复习任务。
     */
    private List<TaskTemplate> selectExamTasksByDaysRemaining(SelectionContext context, int maxDifficulty) {
        List<TaskTemplate> all = repository.findByCategoryAndMaxDifficulty(TaskCategory.EXAM_REVIEW, maxDifficulty);
        List<TaskTemplate> ordered = new ArrayList<>();
        int days = context.daysRemaining();

        if (days >= 6) {
            addIfPresent(ordered, all, "EXAM_001"); // 制定复习计划
            addIfPresent(ordered, all, "EXAM_002"); // 攻克一个知识点
        } else if (days >= 4) {
            addIfPresent(ordered, all, "EXAM_002"); // 攻克知识点
            addIfPresent(ordered, all, "EXAM_003"); // 刷题
        } else if (days >= 2) {
            addIfPresent(ordered, all, "EXAM_003"); // 刷题
            addIfPresent(ordered, all, "EXAM_004"); // 错题回顾
        } else {
            addIfPresent(ordered, all, "EXAM_005"); // 知识框架梳理
        }

        if (ordered.isEmpty()) {
            ordered.addAll(all.stream().limit(2).toList());
        }

        return ordered;
    }

    /**
     * 无明确关键事件时，按压力源匹配任务。
     */
    private List<TaskTemplate> selectByStressSources(SelectionContext context, int maxDifficulty) {
        List<TaskTemplate> result = new ArrayList<>();

        for (String stressSource : context.primaryStressSources()) {
            List<TaskTemplate> matched = repository.findByStressSource(stressSource).stream()
                    .filter(t -> t.difficulty().getLevel() <= maxDifficulty)
                    .filter(t -> t.category() != TaskCategory.BEDTIME_RELAXATION) // 睡前任务单独选
                    .toList();

            // 每个压力源最多取1个，避免重复
            if (!matched.isEmpty()) {
                TaskTemplate pick = matched.get(0);
                if (!result.contains(pick)) {
                    result.add(pick);
                }
            }
        }

        // 如果压力源匹配不到，用拖延启动类任务（通用）
        if (result.isEmpty()) {
            result.addAll(repository.findByCategoryAndMaxDifficulty(TaskCategory.PROCRASTINATION_START, maxDifficulty)
                    .stream().limit(2).toList());
        }

        return result;
    }

    /**
     * 选择睡前放松任务。
     * 轮换使用，避免每天重复同一个。
     */
    private TaskTemplate selectBedtimeTask(SelectionContext context, int maxDifficulty) {
        List<TaskTemplate> bedtime = repository.findByCategoryAndMaxDifficulty(
                TaskCategory.BEDTIME_RELAXATION, maxDifficulty);

        if (bedtime.isEmpty()) {
            return null;
        }

        // 按dayNumber轮换，第7天用最轻松的
        int index = (context.dayNumber() - 1) % bedtime.size();
        return bedtime.get(index);
    }

    /**
     * 确定当天允许的最大难度。
     * 压力高/睡眠差/连续未完成时降低难度上限。
     */
    private int determineMaxDifficulty(SelectionContext context) {
        if (context.stressLevel() >= 8 || context.sleepQuality() <= 1 || context.consecutiveMissedDays() >= 3) {
            return TaskDifficulty.EASY.getLevel();
        }
        if (context.stressLevel() >= 6 || context.sleepQuality() <= 2 || context.consecutiveMissedDays() >= 2) {
            return TaskDifficulty.MEDIUM.getLevel();
        }
        return TaskDifficulty.HARD.getLevel();
    }

    /**
     * 判断是否使用缩小版本。
     * 压力很高或昨天完成率很低时，对中等以上难度任务使用缩小版。
     */
    private boolean shouldUseFallback(SelectionContext context, TaskTemplate template) {
        if (template.difficulty() == TaskDifficulty.EASY) {
            return false; // 简单任务不需要缩小版
        }
        return context.stressLevel() >= 7
                || context.yesterdayCompletionRate() < 0.3
                || context.consecutiveMissedDays() >= 2;
    }

    /**
     * 检查时间预算是否还够加这个任务。
     */
    private boolean hasBudgetFor(List<TaskInstance> current, TaskTemplate candidate, SelectionContext context) {
        int currentMinutes = current.stream().mapToInt(TaskInstance::getActualMinutes).sum();
        int candidateMinutes = shouldUseFallback(context, candidate)
                ? candidate.getFallbackMinutes()
                : candidate.estimatedMinutes();
        return currentMinutes + candidateMinutes <= context.totalTaskBudgetMinutes();
    }

    private void addIfPresent(List<TaskTemplate> list, List<TaskTemplate> pool, String templateId) {
        pool.stream()
                .filter(t -> t.templateId().equals(templateId))
                .findFirst()
                .ifPresent(list::add);
    }

    private TaskInstance instantiate(TaskTemplate template, LocalDate date, int dayNumber, int sortOrder, boolean useFallback) {
        return new TaskInstance(
                template.templateId() + "_" + date,
                template.templateId(),
                template.category(),
                template.title(),
                template.goal(),
                template.estimatedMinutes(),
                template.difficulty(),
                template.suggestedTime(),
                template.steps(),
                template.completionCriteria(),
                useFallback,
                template.fallbackVersion(),
                date,
                dayNumber,
                sortOrder
        );
    }
}
