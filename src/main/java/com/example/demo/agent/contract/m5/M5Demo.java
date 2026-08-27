package com.example.demo.agent.contract.m5;

import com.example.demo.agent.contract.m5.model.*;
import com.example.demo.agent.contract.m5.service.InMemoryTaskTemplateRepository;
import com.example.demo.agent.contract.m5.service.TaskRearranger;
import com.example.demo.agent.contract.m5.service.TaskSelector;

import java.time.LocalDate;
import java.util.List;

/**
 * M5 现实任务模块独立演示入口。
 *
 * <p>无需 Spring Boot / MySQL / MongoDB 即可运行，直接展示：
 * 1. 模板库加载情况
 * 2. 面试7天计划的任务选择
 * 3. 高压状态下的减负效果
 * 4. 动态重排的可解释原因
 *
 * <p>运行方式：java -cp target/classes com.example.demo.agent.contract.M5Demo
 */
public class M5Demo {

    public static void main(String[] args) {
        InMemoryTaskTemplateRepository repo = new InMemoryTaskTemplateRepository();
        TaskSelector selector = new TaskSelector(repo);
        TaskRearranger rearranger = new TaskRearranger(selector);
        LocalDate planStart = LocalDate.now();

        System.out.println("=".repeat(60));
        System.out.println("M5 现实任务模块 - 独立演示");
        System.out.println("=".repeat(60));

        // 1. 模板库统计
        System.out.println("\n【1. 模板库统计】");
        System.out.println("模板总数: " + repo.size());
        for (TaskCategory cat : TaskCategory.values()) {
            System.out.printf("  %s: %d 个%n", cat.getDisplayName(), repo.findByCategory(cat).size());
        }

        // 2. 面试场景 Day1（剩余7天，中度压力）
        System.out.println("\n【2. 面试场景 - Day1（剩余7天，中度压力）】");
        SelectionContext interviewDay1 = new SelectionContext(
                1, 7, 6, SelectionContext.StressTrend.STABLE,
                2, 1.0, 0,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW, 90);
        printContext(interviewDay1);
        List<TaskInstance> day1Tasks = selector.selectForDay(interviewDay1, planStart);
        printTasks(day1Tasks);

        // 3. 高压状态减负
        System.out.println("\n【3. 高压状态 - 减负效果（压力9，睡眠1，连续3天未完成）】");
        SelectionContext highStress = new SelectionContext(
                3, 5, 9, SelectionContext.StressTrend.RISING,
                1, 0.2, 3,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW, 30);
        printContext(highStress);
        List<TaskInstance> stressTasks = selector.selectForDay(highStress, planStart);
        printTasks(stressTasks);
        System.out.printf("  -> 减负后任务数: %d, 总时长: %dmin%n",
                stressTasks.size(),
                stressTasks.stream().mapToInt(TaskInstance::getActualMinutes).sum());

        // 4. 动态重排
        System.out.println("\n【4. 动态重排 - 压力从5升到8】");
        SelectionContext yesterday = new SelectionContext(
                1, 7, 5, SelectionContext.StressTrend.STABLE,
                3, 1.0, 0,
                List.of("求职压力"),
                SelectionContext.KeyEventType.INTERVIEW, 90);
        SelectionContext today = new SelectionContext(
                2, 6, 8, SelectionContext.StressTrend.RISING,
                2, 0.4, 1,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW, 60);
        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = yesterdayTasks.stream()
                .limit(1)
                .map(TaskInstance::instanceId)
                .toList();

        RearrangementResult result = rearranger.rearrange(yesterday, today, yesterdayTasks, completed, planStart);
        System.out.printf("  负担等级: %s%n", result.loadLevel().getDisplayName());
        System.out.printf("  调整后任务数: %d, 总时长: %dmin, 缩小版任务: %d个%n",
                result.adjustedTasks().size(), result.totalMinutes(), result.useFallbackCount());
        System.out.println("  可解释原因:");
        for (AdjustmentReason reason : result.reasons()) {
            System.out.printf("    [%s] %s -> %s%n",
                    reason.reasonType().getDisplayName(),
                    reason.description(),
                    reason.action().getDisplayName());
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("演示完成。M5 模块运行正常。");
        System.out.println("=".repeat(60));
    }

    private static void printContext(SelectionContext ctx) {
        System.out.printf("  第%d天, 剩余%d天, 压力:%d/10(%s), 睡眠:%d/5, 昨日完成率:%.0f%%, 连续未完成:%d天, 预算:%dmin%n",
                ctx.dayNumber(), ctx.daysRemaining(), ctx.stressLevel(),
                ctx.stressLevelTrend().getDisplayName(), ctx.sleepQuality(),
                ctx.yesterdayCompletionRate() * 100, ctx.consecutiveMissedDays(),
                ctx.totalTaskBudgetMinutes());
        System.out.printf("  建议任务数: %d, 需要减负: %s%n",
                ctx.getSuggestedTaskCount(), ctx.needsReducedLoad());
    }

    private static void printTasks(List<TaskInstance> tasks) {
        for (TaskInstance t : tasks) {
            System.out.printf("  [%d] %s - %s | %s | %dmin(实际%dmin) | 难度:%s | 缩小版:%s%n",
                    t.sortOrder(), t.suggestedTime().getDisplayName(), t.title(),
                    t.category().getDisplayName(), t.estimatedMinutes(), t.getActualMinutes(),
                    t.difficulty().getDisplayName(), t.useFallback());
        }
    }
}
