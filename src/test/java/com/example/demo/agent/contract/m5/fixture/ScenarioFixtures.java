package com.example.demo.agent.contract.m5.fixture;

import com.example.demo.agent.contract.m5.model.SelectionContext;

import java.util.List;

/**
 * 场景测试数据工厂。
 *
 * <p>提供求职面试、考试复习、拖延、人际压力等场景的 SelectionContext fixture，
 * 以及各种边界状态（高压、低睡眠、连续未完成等）。
 */
public final class ScenarioFixtures {

    private ScenarioFixtures() {}

    public static SelectionContext interviewDay1() {
        return new SelectionContext(
                1,
                7,
                6,
                SelectionContext.StressTrend.STABLE,
                2,
                1.0,
                0,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW,
                90
        );
    }

    public static SelectionContext interviewDay5HighStress() {
        return new SelectionContext(
                5,
                3,
                8,
                SelectionContext.StressTrend.RISING,
                1,
                0.4,
                1,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW,
                60
        );
    }

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

    public static SelectionContext todayStressRising() {
        return new SelectionContext(
                2,
                6,
                8,
                SelectionContext.StressTrend.RISING,
                2,
                0.4,
                1,
                List.of("求职压力", "睡眠紊乱"),
                SelectionContext.KeyEventType.INTERVIEW,
                60
        );
    }
}
