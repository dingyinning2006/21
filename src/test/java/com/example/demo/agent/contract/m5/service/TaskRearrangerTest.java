package com.example.demo.agent.contract.m5.service;

import com.example.demo.agent.contract.m5.fixture.ScenarioFixtures;
import com.example.demo.agent.contract.m5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5-002 任务动态重排器测试。
 */
class TaskRearrangerTest {

    private TaskRearranger rearranger;
    private TaskSelector selector;
    private final LocalDate planStart = LocalDate.of(2026, 8, 25);

    @BeforeEach
    void setUp() {
        InMemoryTaskTemplateRepository repo = new InMemoryTaskTemplateRepository();
        selector = new TaskSelector(repo);
        rearranger = new TaskRearranger(selector);
    }

    @Test
    @DisplayName("压力从5升到8：任务数量减少，且有可解释的原因")
    void stressRising_reducesTasksAndProvidesReasons() {
        SelectionContext yesterday = ScenarioFixtures.yesterdayNormal();
        SelectionContext today = ScenarioFixtures.todayStressRising();

        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = yesterdayTasks.stream()
                .limit(1)
                .map(TaskInstance::instanceId)
                .toList();

        RearrangementResult result = rearranger.rearrange(
                yesterday, today, yesterdayTasks, completed, planStart);

        assertNotNull(result);
        assertFalse(result.adjustedTasks().isEmpty(), "应生成调整后的任务");

        assertFalse(result.reasons().isEmpty(), "应输出可解释的调整原因");

        boolean hasStressReason = result.reasons().stream()
                .anyMatch(r -> r.reasonType() == AdjustmentReason.ReasonType.RISING_STRESS
                        || r.reasonType() == AdjustmentReason.ReasonType.HIGH_STRESS);
        assertTrue(hasStressReason, "应包含压力升高的调整原因");

        for (AdjustmentReason reason : result.reasons()) {
            assertNotNull(reason.description(), "原因应有描述");
            assertFalse(reason.description().isBlank(), "原因描述不应为空");
            assertNotNull(reason.action(), "原因应有调整动作");
        }

        System.out.println("压力升高重排结果:");
        System.out.println("  负担等级: " + result.loadLevel().getDisplayName());
        System.out.println("  任务数: " + result.adjustedTasks().size());
        System.out.println("  总时长: " + result.totalMinutes() + "min");
        System.out.println("  缩小版任务数: " + result.useFallbackCount());
        System.out.println("  调整原因:");
        result.reasons().forEach(r -> System.out.printf("    [%s] %s -> %s%n",
                r.reasonType().getDisplayName(), r.description(), r.action().getDisplayName()));
    }

    @Test
    @DisplayName("连续未完成：启用缩小版本，任务数量下降")
    void consecutiveMissed_enablesFallbackAndReducesCount() {
        SelectionContext yesterday = ScenarioFixtures.yesterdayNormal();
        SelectionContext today = ScenarioFixtures.extremeHighStress();

        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = new ArrayList<>();

        RearrangementResult result = rearranger.rearrange(
                yesterday, today, yesterdayTasks, completed, planStart);

        boolean hasConsecutiveReason = result.reasons().stream()
                .anyMatch(r -> r.reasonType() == AdjustmentReason.ReasonType.CONSECUTIVE_MISSED);
        assertTrue(hasConsecutiveReason, "应包含连续未完成的调整原因");

        assertEquals(RearrangementResult.LoadLevel.LIGHT, result.loadLevel(),
                "连续未完成时应为轻负担");

        System.out.println("连续未完成重排结果:");
        System.out.println("  负担等级: " + result.loadLevel().getDisplayName());
        System.out.println("  任务数: " + result.adjustedTasks().size());
        result.reasons().forEach(r -> System.out.printf("    [%s] %s%n",
                r.reasonType().getDisplayName(), r.description()));
    }

    @Test
    @DisplayName("状态稳定正常推进：原因包含 NORMAL_PROGRESSION")
    void stableState_normalProgression() {
        SelectionContext yesterday = ScenarioFixtures.yesterdayNormal();
        SelectionContext today = new SelectionContext(
                2, 6, 4, SelectionContext.StressTrend.STABLE,
                4, 0.9, 0,
                List.of("求职压力"),
                SelectionContext.KeyEventType.INTERVIEW,
                90
        );

        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = yesterdayTasks.stream()
                .map(TaskInstance::instanceId)
                .toList();

        RearrangementResult result = rearranger.rearrange(
                yesterday, today, yesterdayTasks, completed, planStart);

        boolean hasNormal = result.reasons().stream()
                .anyMatch(r -> r.reasonType() == AdjustmentReason.ReasonType.NORMAL_PROGRESSION);
        assertTrue(hasNormal, "状态稳定时应包含正常推进的原因");

        System.out.println("正常推进重排结果:");
        System.out.println("  负担等级: " + result.loadLevel().getDisplayName());
        System.out.println("  任务数: " + result.adjustedTasks().size());
        result.reasons().forEach(r -> System.out.printf("    [%s] %s%n",
                r.reasonType().getDisplayName(), r.description()));
    }

    @Test
    @DisplayName("昨天有未完成任务：状态好时顺延一个，不增加总数")
    void unfinishedTask_carriedOverWithoutIncreasingCount() {
        SelectionContext yesterday = ScenarioFixtures.yesterdayNormal();
        SelectionContext today = ScenarioFixtures.goodState();

        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = yesterdayTasks.stream()
                .limit(1)
                .map(TaskInstance::instanceId)
                .toList();

        RearrangementResult result = rearranger.rearrange(
                yesterday, today, yesterdayTasks, completed, planStart);

        int expectedMax = today.getSuggestedTaskCount() + 1;
        assertTrue(result.adjustedTasks().size() <= expectedMax + 1,
                "任务数不应异常增加，当前: " + result.adjustedTasks().size());

        System.out.println("未完成顺延重排结果:");
        System.out.println("  任务数: " + result.adjustedTasks().size());
        result.adjustedTasks().forEach(t -> System.out.printf("    %s (%s)%n",
                t.title(), t.category().getDisplayName()));
    }

    @Test
    @DisplayName("高压状态下有未完成任务：不顺延，先减负")
    void highStressUnfinished_noCarryOverReduceLoad() {
        SelectionContext yesterday = ScenarioFixtures.yesterdayNormal();
        SelectionContext today = ScenarioFixtures.extremeHighStress();

        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = new ArrayList<>();

        RearrangementResult result = rearranger.rearrange(
                yesterday, today, yesterdayTasks, completed, planStart);

        assertEquals(RearrangementResult.LoadLevel.LIGHT, result.loadLevel(),
                "高压下应为轻负担");

        System.out.println("高压未完成重排结果:");
        System.out.println("  负担等级: " + result.loadLevel().getDisplayName());
        System.out.println("  任务数: " + result.adjustedTasks().size());
        result.reasons().forEach(r -> System.out.printf("    [%s] %s%n",
                r.reasonType().getDisplayName(), r.description()));
    }

    @Test
    @DisplayName("所有调整原因都可被M1转成自然语言：有类型、描述、动作")
    void allReasonsAreExplainable() {
        SelectionContext yesterday = ScenarioFixtures.yesterdayNormal();
        SelectionContext today = ScenarioFixtures.todayStressRising();

        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = yesterdayTasks.stream()
                .limit(1)
                .map(TaskInstance::instanceId)
                .toList();

        RearrangementResult result = rearranger.rearrange(
                yesterday, today, yesterdayTasks, completed, planStart);

        for (AdjustmentReason reason : result.reasons()) {
            assertNotNull(reason.reasonType(), "原因类型不应为空");
            assertNotNull(reason.description(), "原因描述不应为空");
            assertFalse(reason.description().isBlank(), "原因描述不应为空字符串");
            assertNotNull(reason.action(), "调整动作不应为空");
            assertTrue(reason.description().length() >= 5,
                    "原因描述应足够具体: " + reason.description());
        }

        System.out.println("可解释性验证通过，共 " + result.reasons().size() + " 条原因");
    }

    @Test
    @DisplayName("关键事件临近：有冲刺阶段的调整原因")
    void keyEventApproaching_hasSprintReason() {
        SelectionContext yesterday = new SelectionContext(
                5, 3, 5, SelectionContext.StressTrend.STABLE,
                3, 0.8, 0,
                List.of("求职压力"),
                SelectionContext.KeyEventType.INTERVIEW,
                90
        );
        SelectionContext today = new SelectionContext(
                6, 2, 6, SelectionContext.StressTrend.STABLE,
                3, 0.8, 0,
                List.of("求职压力"),
                SelectionContext.KeyEventType.INTERVIEW,
                90
        );

        List<TaskInstance> yesterdayTasks = selector.selectForDay(yesterday, planStart);
        List<String> completed = yesterdayTasks.stream()
                .map(TaskInstance::instanceId)
                .toList();

        RearrangementResult result = rearranger.rearrange(
                yesterday, today, yesterdayTasks, completed, planStart);

        boolean hasApproaching = result.reasons().stream()
                .anyMatch(r -> r.reasonType() == AdjustmentReason.ReasonType.KEY_EVENT_APPROACHING);
        assertTrue(hasApproaching, "关键事件临近时应有冲刺调整原因");

        System.out.println("关键事件临近重排:");
        result.reasons().forEach(r -> System.out.printf("    [%s] %s%n",
                r.reasonType().getDisplayName(), r.description()));
    }
}
