package com.example.demo.agent.contract.m5.service;

import com.example.demo.agent.contract.m5.fixture.ScenarioFixtures;
import com.example.demo.agent.contract.m5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5-002 任务选择器测试。
 */
class TaskSelectorTest {

    private TaskSelector selector;
    private final LocalDate planStart = LocalDate.of(2026, 8, 25);

    @BeforeEach
    void setUp() {
        InMemoryTaskTemplateRepository repo = new InMemoryTaskTemplateRepository();
        selector = new TaskSelector(repo);
    }

    @Test
    @DisplayName("面试剩余7天（Day1）：生成简历梳理+公司研究，有顺序")
    void interviewDay1_remaining7_generatesOrderedTasks() {
        SelectionContext ctx = ScenarioFixtures.interviewDay1();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        assertFalse(tasks.isEmpty(), "应生成任务");

        List<TaskInstance> realityTasks = tasks.stream()
                .filter(t -> t.category() == TaskCategory.INTERVIEW_PREP)
                .toList();

        assertTrue(realityTasks.size() >= 1, "应包含面试准备任务");

        assertEquals("INTERVIEW_001", realityTasks.get(0).templateId(),
                "剩余7天时第一个任务应为简历梳理");

        System.out.println("面试Day1（剩余7天）任务:");
        tasks.forEach(t -> System.out.printf("  [%s] %s (%dmin, %s)%n",
                t.suggestedTime(), t.title(), t.getActualMinutes(), t.difficulty()));
    }

    @Test
    @DisplayName("面试剩余3天（Day5）：生成模拟面试+常见问题，进入冲刺")
    void interviewDay5_remaining3_generatesSprintTasks() {
        SelectionContext ctx = ScenarioFixtures.interviewDay5HighStress();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        List<TaskInstance> realityTasks = tasks.stream()
                .filter(t -> t.category() == TaskCategory.INTERVIEW_PREP)
                .toList();

        boolean hasMock = realityTasks.stream()
                .anyMatch(t -> t.templateId().equals("INTERVIEW_005"));
        assertTrue(hasMock, "剩余3天应包含模拟面试任务");

        System.out.println("面试Day5（剩余3天）任务:");
        tasks.forEach(t -> System.out.printf("  [%s] %s (%dmin, %s)%n",
                t.suggestedTime(), t.title(), t.getActualMinutes(), t.difficulty()));
    }

    @Test
    @DisplayName("面试剩余1天（Day7）：只生成轻量复习，不做新内容")
    void interviewDay7_remaining1_generatesLightReview() {
        SelectionContext ctx = ScenarioFixtures.interviewDay7Before();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        List<TaskInstance> realityTasks = tasks.stream()
                .filter(t -> t.category() == TaskCategory.INTERVIEW_PREP)
                .toList();

        boolean hasLightReview = realityTasks.stream()
                .anyMatch(t -> t.templateId().equals("INTERVIEW_006"));
        assertTrue(hasLightReview, "剩余1天应包含轻量复习任务");

        boolean hasHardTask = realityTasks.stream()
                .anyMatch(t -> t.difficulty() == TaskDifficulty.HARD);
        assertFalse(hasHardTask, "面试前1天不应有高难度任务");

        System.out.println("面试Day7（剩余1天）任务:");
        tasks.forEach(t -> System.out.printf("  [%s] %s (%dmin, %s)%n",
                t.suggestedTime(), t.title(), t.getActualMinutes(), t.difficulty()));
    }

    @Test
    @DisplayName("高压状态：任务数量减少，难度上限降低")
    void highStress_reducesTaskCountAndDifficulty() {
        SelectionContext normal = ScenarioFixtures.goodState();
        SelectionContext highStress = ScenarioFixtures.extremeHighStress();

        List<TaskInstance> normalTasks = selector.selectForDay(normal, planStart);
        List<TaskInstance> stressTasks = selector.selectForDay(highStress, planStart);

        assertTrue(stressTasks.size() <= normalTasks.size(),
                "高压状态任务数(" + stressTasks.size() + ")应不多于正常状态(" + normalTasks.size() + ")");

        boolean hasHard = stressTasks.stream()
                .anyMatch(t -> t.difficulty() == TaskDifficulty.HARD);
        assertFalse(hasHard, "高压状态不应有HARD难度任务");

        System.out.println("正常状态: " + normalTasks.size() + " 个任务");
        System.out.println("高压状态: " + stressTasks.size() + " 个任务");
    }

    @Test
    @DisplayName("连续未完成：启用缩小版本（fallback）")
    void consecutiveMissed_enablesFallbackVersion() {
        SelectionContext ctx = ScenarioFixtures.extremeHighStress();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        long mediumOrHard = tasks.stream()
                .filter(t -> t.difficulty() != TaskDifficulty.EASY)
                .count();
        long fallbackCount = tasks.stream()
                .filter(TaskInstance::useFallback)
                .count();

        if (mediumOrHard > 0) {
            assertTrue(fallbackCount > 0,
                    "连续未完成时，中等以上难度任务应启用缩小版本");
        }

        System.out.println("连续未完成状态:");
        tasks.forEach(t -> System.out.printf("  %s - %s (缩小版: %s, 实际%dmin)%n",
                t.category(), t.title(), t.useFallback(), t.getActualMinutes()));
    }

    @Test
    @DisplayName("每天必须包含一个睡前放松任务")
    void everyDayHasBedtimeTask() {
        SelectionContext[] contexts = {
                ScenarioFixtures.interviewDay1(),
                ScenarioFixtures.examDay1(),
                ScenarioFixtures.procrastinationScenario(),
                ScenarioFixtures.interpersonalScenario(),
                ScenarioFixtures.extremeHighStress()
        };

        for (SelectionContext ctx : contexts) {
            List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);
            boolean hasBedtime = tasks.stream()
                    .anyMatch(t -> t.category() == TaskCategory.BEDTIME_RELAXATION);
            assertTrue(hasBedtime,
                    "场景 " + ctx.keyEventType() + " 应包含睡前放松任务（压力:" + ctx.stressLevel() + ")");
        }
    }

    @Test
    @DisplayName("任务排序：晨间任务在前，睡前任务在最后")
    void tasksAreOrderedByTimeOfDay() {
        SelectionContext ctx = ScenarioFixtures.interviewDay1();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        TaskInstance last = tasks.get(tasks.size() - 1);
        assertEquals(TaskCategory.BEDTIME_RELAXATION, last.category(),
                "最后一个任务应为睡前放松");

        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(i, tasks.get(i).sortOrder(), "sortOrder应连续");
        }
    }

    @Test
    @DisplayName("考试场景：剩余7天生成复习计划+知识点攻克")
    void examDay1_generatesReviewPlan() {
        SelectionContext ctx = ScenarioFixtures.examDay1();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        List<TaskInstance> examTasks = tasks.stream()
                .filter(t -> t.category() == TaskCategory.EXAM_REVIEW)
                .toList();

        assertTrue(examTasks.size() >= 1, "考试场景应包含复习任务");

        boolean hasPlan = examTasks.stream()
                .anyMatch(t -> t.templateId().equals("EXAM_001"));
        assertTrue(hasPlan, "考试第1天应包含制定复习计划任务");

        System.out.println("考试Day1任务:");
        tasks.forEach(t -> System.out.printf("  [%s] %s (%dmin)%n",
                t.suggestedTime(), t.title(), t.getActualMinutes()));
    }

    @Test
    @DisplayName("拖延场景：匹配到拖延启动类任务")
    void procrastinationScenario_matchesProcrastinationTasks() {
        SelectionContext ctx = ScenarioFixtures.procrastinationScenario();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        boolean hasProcrastination = tasks.stream()
                .anyMatch(t -> t.category() == TaskCategory.PROCRASTINATION_START);
        assertTrue(hasProcrastination, "拖延场景应包含拖延启动任务");

        System.out.println("拖延场景任务:");
        tasks.forEach(t -> System.out.printf("  [%s] %s (%dmin)%n",
                t.suggestedTime(), t.title(), t.getActualMinutes()));
    }

    @Test
    @DisplayName("人际压力场景：匹配到人际沟通类任务")
    void interpersonalScenario_matchesInterpersonalTasks() {
        SelectionContext ctx = ScenarioFixtures.interpersonalScenario();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        boolean hasInterpersonal = tasks.stream()
                .anyMatch(t -> t.category() == TaskCategory.INTERPERSONAL);
        assertTrue(hasInterpersonal, "人际压力场景应包含人际沟通任务");

        System.out.println("人际压力场景任务:");
        tasks.forEach(t -> System.out.printf("  [%s] %s (%dmin)%n",
                t.suggestedTime(), t.title(), t.getActualMinutes()));
    }

    @Test
    @DisplayName("时间预算：任务总时长不超过预算")
    void totalMinutesWithinBudget() {
        SelectionContext ctx = ScenarioFixtures.interviewDay1();
        List<TaskInstance> tasks = selector.selectForDay(ctx, planStart);

        int totalMinutes = tasks.stream().mapToInt(TaskInstance::getActualMinutes).sum();
        assertTrue(totalMinutes <= ctx.totalTaskBudgetMinutes(),
                "任务总时长(" + totalMinutes + "min)不应超过预算(" + ctx.totalTaskBudgetMinutes() + "min)");

        System.out.println("预算: " + ctx.totalTaskBudgetMinutes() + "min, 实际: " + totalMinutes + "min");
    }
}
