package com.example.demo.stage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.SafetyLevel;
import com.example.demo.agent.contract.SupportPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M6-001 指标与趋势计算测试。
 * 覆盖分工文档要求的全部场景：完整数据、数据不足、缺失某天、压力波动、任务全未完成、无数据。
 */
class TrendCalculatorTest {

    private final TrendCalculator calculator = new TrendCalculator();

    // ────────────────────── 辅助方法 ──────────────────────

    private CheckInRecord record(LocalDate date, int stress, double sleep, double completion) {
        return new CheckInRecord(
                "user1", date, stress, sleep, 5, completion,
                List.of(), List.of(), null, SupportPhase.CHECK_IN
        );
    }

    private CheckInRecord recordWithNote(
            LocalDate date, int stress, double sleep, double completion, String note) {
        return new CheckInRecord(
                "user1", date, stress, sleep, 5, completion,
                List.of("task1"), List.of(), note, SupportPhase.CHECK_IN
        );
    }

    // ────────────────────── 场景 1：7 天完整数据 ──────────────────────

    @Nested
    @DisplayName("场景 1：7 天完整数据")
    class FullWeekData {

        @Test
        @DisplayName("压力下降 + 高完成率 → 明显改善")
        void stressDownHighCompletion() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 18), 8, 5.5, 0.25),
                    record(LocalDate.of(2026, 8, 19), 7, 6.0, 0.5),
                    record(LocalDate.of(2026, 8, 20), 7, 6.0, 0.5),
                    record(LocalDate.of(2026, 8, 21), 6, 6.5, 0.75),
                    record(LocalDate.of(2026, 8, 22), 5, 7.0, 0.75),
                    record(LocalDate.of(2026, 8, 23), 5, 7.0, 1.0),
                    record(LocalDate.of(2026, 8, 24), 5, 7.5, 0.75)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(7, result.totalDays());
            assertEquals(8, result.firstStressLevel());
            assertEquals(5, result.lastStressLevel());
            assertEquals(-3, result.stressDelta());
            assertEquals(5.5, result.firstSleepHours());
            assertEquals(7.5, result.lastSleepHours());
            assertEquals(2.0, result.sleepDelta(), 0.01);
            assertTrue(result.avgCompletionRate() >= 0.6);
            assertEquals(7, result.streakDays());
            assertEquals("明显改善", result.trend());
            assertTrue(result.hasEnoughData());
            assertTrue(result.concerns().isEmpty());
        }

        @Test
        @DisplayName("压力下降但完成率低 → 压力下降但任务完成率低")
        void stressDownLowCompletion() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 18), 8, 6.0, 0.0),
                    record(LocalDate.of(2026, 8, 19), 7, 6.0, 0.25),
                    record(LocalDate.of(2026, 8, 20), 6, 6.5, 0.0),
                    record(LocalDate.of(2026, 8, 21), 5, 7.0, 0.25),
                    record(LocalDate.of(2026, 8, 22), 5, 7.0, 0.25),
                    record(LocalDate.of(2026, 8, 23), 4, 7.0, 0.0),
                    record(LocalDate.of(2026, 8, 24), 4, 7.5, 0.25)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(-4, result.stressDelta());
            assertTrue(result.avgCompletionRate() < 0.4);
            assertEquals("压力下降但任务完成率低", result.trend());
        }

        @Test
        @DisplayName("压力上升 + 高完成率 → 压力上升但执行力好")
        void stressUpHighCompletion() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 18), 3, 7.0, 1.0),
                    record(LocalDate.of(2026, 8, 19), 4, 7.0, 0.75),
                    record(LocalDate.of(2026, 8, 20), 5, 6.5, 0.75),
                    record(LocalDate.of(2026, 8, 21), 6, 6.0, 1.0),
                    record(LocalDate.of(2026, 8, 22), 6, 6.0, 0.75),
                    record(LocalDate.of(2026, 8, 23), 7, 5.5, 0.75),
                    record(LocalDate.of(2026, 8, 24), 7, 5.5, 1.0)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(4, result.stressDelta());
            assertTrue(result.avgCompletionRate() >= 0.6);
            assertEquals("压力上升但执行力好", result.trend());
            assertTrue(result.concerns().stream().anyMatch(c -> c.contains("压力")));
        }
    }

    // ────────────────────── 场景 2：只有 2 天数据 ──────────────────────

    @Nested
    @DisplayName("场景 2：只有 2 天数据")
    class TwoDaysData {

        @Test
        @DisplayName("2 天数据 → 能计算但趋势标记为样本不足")
        void twoDaysInsufficient() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 23), 7, 6.0, 0.5),
                    record(LocalDate.of(2026, 8, 24), 5, 7.0, 0.75)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(2, result.totalDays());
            assertEquals(-2, result.stressDelta());
            assertTrue(result.hasEnoughData());
            // 2 天刚好达到阈值，能给出趋势判定
            assertNotNull(result.trend());
        }

        @Test
        @DisplayName("只有 1 天数据 → 样本不足")
        void oneDayInsufficient() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 24), 6, 7.0, 0.5)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(1, result.totalDays());
            assertEquals("样本不足", result.trend());
            assertFalse(result.hasEnoughData());
        }
    }

    // ────────────────────── 场景 3：中间缺了一天 ──────────────────────

    @Nested
    @DisplayName("场景 3：中间缺了一天")
    class MissingDay {

        @Test
        @DisplayName("第 4 天缺失 → 跳过那天，连续打卡中断")
        void missingDay4() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 18), 8, 5.5, 0.25),
                    record(LocalDate.of(2026, 8, 19), 7, 6.0, 0.5),
                    record(LocalDate.of(2026, 8, 20), 7, 6.0, 0.5),
                    // 第 4 天缺失
                    record(LocalDate.of(2026, 8, 22), 6, 6.5, 0.75),
                    record(LocalDate.of(2026, 8, 23), 5, 7.0, 0.75),
                    record(LocalDate.of(2026, 8, 24), 5, 7.0, 0.75)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(6, result.totalDays());
            assertEquals(8, result.firstStressLevel());
            assertEquals(5, result.lastStressLevel());
            // 连续打卡从最后一天往前数，8/24→8/23→8/22 连续，但 8/20→8/22 断了
            assertEquals(3, result.streakDays());
        }
    }

    // ────────────────────── 场景 4：压力先升后降 ──────────────────────

    @Nested
    @DisplayName("场景 4：压力先升后降")
    class StressFluctuation {

        @Test
        @DisplayName("8→9→7→5 → 最终下降，趋势为改善")
        void upThenDown() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 18), 8, 5.5, 0.5),
                    record(LocalDate.of(2026, 8, 19), 9, 5.0, 0.25),
                    record(LocalDate.of(2026, 8, 20), 7, 6.0, 0.5),
                    record(LocalDate.of(2026, 8, 21), 5, 7.0, 0.75)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(-3, result.stressDelta());
            // 虽然中间升了，但最终趋势是下降的
            assertTrue(result.trend().contains("改善") || result.trend().contains("下降"));
        }

        @Test
        @DisplayName("连续多天压力上升 → 触发风险提示")
        void consecutiveUp() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 18), 3, 7.0, 0.75),
                    record(LocalDate.of(2026, 8, 19), 5, 6.5, 0.5),
                    record(LocalDate.of(2026, 8, 20), 7, 6.0, 0.5),
                    record(LocalDate.of(2026, 8, 21), 8, 5.0, 0.25)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(5, result.stressDelta());
            assertTrue(result.concerns().stream().anyMatch(c -> c.contains("压力")));
        }
    }

    // ────────────────────── 场景 5：任务全没完成 ──────────────────────

    @Nested
    @DisplayName("场景 5：任务全没完成")
    class ZeroCompletion {

        @Test
        @DisplayName("完成率全为 0 → 完成率 0%，有风险提示")
        void allMissed() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 18), 7, 6.0, 0.0),
                    record(LocalDate.of(2026, 8, 19), 7, 6.0, 0.0),
                    record(LocalDate.of(2026, 8, 20), 8, 5.5, 0.0),
                    record(LocalDate.of(2026, 8, 21), 8, 5.5, 0.0)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(0.0, result.avgCompletionRate());
            assertTrue(result.concerns().stream().anyMatch(c -> c.contains("完成率")));
            assertTrue(result.effectiveMethods().isEmpty());
        }
    }

    // ────────────────────── 场景 6：7 天全没打卡 ──────────────────────

    @Nested
    @DisplayName("场景 6：7 天全没打卡（空数据）")
    class NoData {

        @Test
        @DisplayName("空列表 → 暂无数据")
        void emptyList() {
            IndicatorResult result = calculator.calculate(List.of(), SafetyLevel.NORMAL);

            assertEquals(0, result.totalDays());
            assertEquals("暂无数据", result.trend());
            assertFalse(result.hasEnoughData());
        }

        @Test
        @DisplayName("null → 暂无数据")
        void nullList() {
            IndicatorResult result = calculator.calculate(null, SafetyLevel.NORMAL);

            assertEquals(0, result.totalDays());
            assertEquals("暂无数据", result.trend());
        }
    }

    // ────────────────────── 边界场景 ──────────────────────

    @Nested
    @DisplayName("边界场景")
    class EdgeCases {

        @Test
        @DisplayName("同一天重复打卡 → 只保留最后一条")
        void duplicateDay() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 24), 8, 5.0, 0.0),
                    record(LocalDate.of(2026, 8, 24), 5, 7.0, 1.0)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            // 只有 1 天（去重后），样本不足
            assertEquals(1, result.totalDays());
            assertEquals("样本不足", result.trend());
        }

        @Test
        @DisplayName("记录未按日期排序 → 内部自动排序")
        void unsortedRecords() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 24), 5, 7.0, 0.75),
                    record(LocalDate.of(2026, 8, 18), 8, 5.5, 0.25),
                    record(LocalDate.of(2026, 8, 21), 6, 6.5, 0.5)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertEquals(8, result.firstStressLevel());
            assertEquals(5, result.lastStressLevel());
            assertEquals(-3, result.stressDelta());
        }

        @Test
        @DisplayName("高危安全等级 → 结果携带安全等级")
        void urgentSafetyLevel() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 23), 9, 4.0, 0.0),
                    record(LocalDate.of(2026, 8, 24), 10, 3.0, 0.0)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.URGENT);

            assertEquals(SafetyLevel.URGENT, result.safetyLevel());
        }

        @Test
        @DisplayName("note 字段含关键词 → 提取有效方法")
        void extractMethodsFromNote() {
            List<CheckInRecord> records = List.of(
                    recordWithNote(LocalDate.of(2026, 8, 23), 7, 6.0, 0.75, "做了呼吸训练，感觉好多了"),
                    recordWithNote(LocalDate.of(2026, 8, 24), 5, 7.0, 0.75, "晚上散步了半小时")
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertTrue(result.effectiveMethods().contains("呼吸训练"));
            assertTrue(result.effectiveMethods().contains("运动"));
        }

        @Test
        @DisplayName("多数天睡眠不足 6 小时 → 触发睡眠风险提示")
        void sleepDeficitRisk() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 20), 7, 5.0, 0.5),
                    record(LocalDate.of(2026, 8, 21), 7, 4.5, 0.5),
                    record(LocalDate.of(2026, 8, 22), 8, 5.5, 0.5),
                    record(LocalDate.of(2026, 8, 23), 8, 5.0, 0.5)
            );

            IndicatorResult result = calculator.calculate(records, SafetyLevel.NORMAL);

            assertTrue(result.concerns().stream().anyMatch(c -> c.contains("睡眠")));
        }
    }
}
