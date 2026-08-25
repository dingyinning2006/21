package com.example.demo.m5.fixture;

import com.example.demo.m5.model.SelectionContext;

import java.util.List;

/**
 * 场景测试数据工厂。
 *
 * <p>提供求职面试、考试复习、拖延、人际压力等场景的 SelectionContext fixture，
 * 以及各种边界状态（高压、低睡眠、连续未完成等）。
 */
public final class ScenarioFixtures {

    private ScenarioFixtures() {}

    /**
     * 面试场景：剩余7天，中度压力，睡眠一般，状态尚可。
     * 对应演示案例："我下周要面试，最近每天失眠、焦虑"。
     */
    public static SelectionContext interviewDay1() {
        return new SelectionContext(
                1,                           // dayNumber
                7,                           // daysRemaining - 面试还有7天
                6,                           // stressLevel - 中度偏高
                SelectionContext.StressTrend.STABLE,
                2,                           // sleepQuality - 睡眠较差
                1.0,                         // yesterdayCompletionRate - 第一天默认全完成
                0,                           // consecutiveMissedDays
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW,
                90                           // totalTaskBudgetMinutes
        );
    }

    /**
     * 面试场景：剩余3天，压力上升，睡眠差。
     */
    public static SelectionContext interviewDay5HighStress() {
        return new SelectionContext(
                5,
                3,
                8,                           // 高压
                SelectionContext.StressTrend.RISING,
                1,                           // 睡眠很差
                0.4,                         // 昨天完成率低
                1,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW,
                60
        );
    }

    /**
     * 面试场景：剩余1天，考前/面前轻量状态。
     */
    public static SelectionContext interviewDay7Before() {
        return new SelectionContext(
                7,
                1,
                5,
                SelectionContext.StressTrend.FALLING,
                3,
                0.8,
                0,
                List.of("求职压力"),
                SelectionContext.KeyEventType.INTERVIEW,
                60
        );
    }

    /**
     * 考试场景：剩余7天，中度焦虑。
     */
    public static SelectionContext examDay1() {
        return new SelectionContext(
                1,
                7,
                5,
                SelectionContext.StressTrend.STABLE,
                3,
                1.0,
                0,
                List.of("考试焦虑"),
                SelectionContext.KeyEventType.EXAM,
                120
        );
    }

    /**
     * 拖延场景：无明确关键事件，主要压力源是拖延。
     */
    public static SelectionContext procrastinationScenario() {
        return new SelectionContext(
                1,
                7,
                4,
                SelectionContext.StressTrend.STABLE,
                3,
                1.0,
                0,
                List.of("拖延行为"),
                SelectionContext.KeyEventType.NONE,
                60
        );
    }

    /**
     * 人际压力场景：无明确关键事件，主要压力源是人际关系。
     */
    public static SelectionContext interpersonalScenario() {
        return new SelectionContext(
                1,
                7,
                5,
                SelectionContext.StressTrend.STABLE,
                3,
                1.0,
                0,
                List.of("人际压力"),
                SelectionContext.KeyEventType.NONE,
                60
        );
    }

    /**
     * 高压状态：压力等级9，睡眠1，连续3天未完成。
     * 用于测试减负规则。
     */
    public static SelectionContext extremeHighStress() {
        return new SelectionContext(
                3,
                5,
                9,
                SelectionContext.StressTrend.RISING,
                1,
                0.2,
                3,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW,
                30
        );
    }

    /**
     * 良好状态：压力低，睡眠好，完成率高。
     * 用于测试满负荷任务分配。
     */
    public static SelectionContext goodState() {
        return new SelectionContext(
                2,
                6,
                3,
                SelectionContext.StressTrend.FALLING,
                5,
                0.9,
                0,
                List.of("求职压力"),
                SelectionContext.KeyEventType.INTERVIEW,
                120
        );
    }

    /**
     * 昨天的上下文（用于对比测试动态重排）。
     */
    public static SelectionContext yesterdayNormal() {
        return new SelectionContext(
                1,
                7,
                5,
                SelectionContext.StressTrend.STABLE,
                3,
                1.0,
                0,
                List.of("求职压力"),
                SelectionContext.KeyEventType.INTERVIEW,
                90
        );
    }

    /**
     * 今天压力上升的上下文（用于对比测试动态重排）。
     */
    public static SelectionContext todayStressRising() {
        return new SelectionContext(
                2,
                6,
                8,                           // 从5升到8
                SelectionContext.StressTrend.RISING,
                2,                           // 睡眠变差
                0.4,                         // 昨天完成率低
                1,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW,
                60
        );
    }
}
