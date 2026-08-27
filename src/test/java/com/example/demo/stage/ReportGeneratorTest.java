package com.example.demo.stage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.SafetyLevel;
import com.example.demo.agent.contract.StageReport;
import com.example.demo.agent.contract.SupportPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M6-002 阶段报告生成测试。
 * 验证报告结构完整性、约束（无诊断语言、安全兼容）和各种输入场景。
 */
class ReportGeneratorTest {

    private final TrendCalculator calculator = new TrendCalculator();
    private final ReportGenerator generator = new ReportGenerator();

    private static final LocalDate START = LocalDate.of(2026, 8, 18);
    private static final LocalDate END = LocalDate.of(2026, 8, 24);
    private static final String USER_ID = "user1";

    // ────────────────────── 辅助方法 ──────────────────────

    private CheckInRecord record(LocalDate date, int stress, double sleep, double completion) {
        return new CheckInRecord(
                USER_ID, date, stress, sleep, 5, completion,
                List.of(), List.of(), null, SupportPhase.CHECK_IN
        );
    }

    // ────────────────────── 正常场景 ──────────────────────

    @Nested
    @DisplayName("正常报告生成")
    class NormalReport {

        @Test
        @DisplayName("7 天完整数据 → 报告结构完整")
        void fullWeekReport() {
            List<CheckInRecord> records = List.of(
                    record(START, 8, 5.5, 0.25),
                    record(START.plusDays(1), 7, 6.0, 0.5),
                    record(START.plusDays(2), 7, 6.0, 0.5),
                    record(START.plusDays(3), 6, 6.5, 0.75),
                    record(START.plusDays(4), 5, 7.0, 0.75),
                    record(START.plusDays(5), 5, 7.0, 1.0),
                    record(END, 5, 7.5, 0.75)
            );

            IndicatorResult indicators = calculator.calculate(records, SafetyLevel.NORMAL);
            StageReport report = generator.generate(indicators, START, END, USER_ID);

            assertEquals(USER_ID, report.userId());
            assertEquals(START, report.startDate());
            assertEquals(END, report.endDate());
            assertEquals(SafetyLevel.NORMAL, report.safetyLevel());
            assertEquals(-3, report.stressDelta());
            assertEquals(2.0, report.sleepDelta(), 0.01);
            assertTrue(report.completionRate() >= 0.5);
            assertFalse(report.observedChanges().isEmpty());
            assertFalse(report.nextStepSuggestions().isEmpty());
            assertFalse(report.summary().isBlank());
        }

        @Test
        @DisplayName("纯文本报告可读且包含关键信息")
        void plainTextReport() {
            List<CheckInRecord> records = List.of(
                    record(START, 8, 5.5, 0.25),
                    record(END, 5, 7.5, 0.75)
            );

            IndicatorResult indicators = calculator.calculate(records, SafetyLevel.NORMAL);
            String text = generator.generatePlainText(indicators, START, END);

            // 包含关键段落
            assertTrue(text.contains("压力变化"));
            assertTrue(text.contains("睡眠情况"));
            assertTrue(text.contains("任务完成率"));
            assertTrue(text.contains("连续打卡"));
            assertTrue(text.contains("整体趋势"));
            // 不包含诊断语言
            assertFalse(text.contains("焦虑症"));
            assertFalse(text.contains("抑郁症"));
            assertFalse(text.contains("确诊"));
        }
    }

    // ────────────────────── 数据不足场景 ──────────────────────

    @Nested
    @DisplayName("数据不足场景")
    class InsufficientData {

        @Test
        @DisplayName("只有 1 天 → 报告标记样本不足")
        void oneDayReport() {
            List<CheckInRecord> records = List.of(
                    record(LocalDate.of(2026, 8, 24), 6, 7.0, 0.5)
            );

            IndicatorResult indicators = calculator.calculate(records, SafetyLevel.NORMAL);
            StageReport report = generator.generate(indicators, LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 24), USER_ID);

            assertEquals(1, report.observedChanges().size());
            // summary 里应提示数据不足
            assertTrue(report.summary().contains("不足") || report.summary().contains("样本"));
        }

        @Test
        @DisplayName("空数据 → 报告正常生成，提示暂无数据")
        void emptyReport() {
            IndicatorResult indicators = calculator.calculate(List.of(), SafetyLevel.NORMAL);

            StageReport report = generator.generate(indicators, START, END, USER_ID);

            assertEquals(USER_ID, report.userId());
            assertEquals(SafetyLevel.NORMAL, report.safetyLevel());
            assertTrue(report.summary().contains("暂无"));
            assertFalse(report.nextStepSuggestions().isEmpty());
            assertTrue(report.observedChanges().stream().anyMatch(c -> c.contains("暂无")));
        }
    }

    // ────────────────────── 安全状态兼容 ──────────────────────

    @Nested
    @DisplayName("安全状态兼容性")
    class SafetyCompatibility {

        @Test
        @DisplayName("高危状态 → 不给普通任务建议")
        void urgentSafety() {
            List<CheckInRecord> records = List.of(
                    record(START, 9, 4.0, 0.0),
                    record(END, 10, 3.0, 0.0)
            );

            IndicatorResult indicators = calculator.calculate(records, SafetyLevel.URGENT);
            StageReport report = generator.generate(indicators, START, END, USER_ID);

            assertEquals(SafetyLevel.URGENT, report.safetyLevel());
            // 高危状态下建议应该包含安全相关内容
            String allSuggestions = String.join(" ", report.nextStepSuggestions());
            assertTrue(allSuggestions.contains("安全") || allSuggestions.contains("热线")
                            || allSuggestions.contains("信任"),
                    "高危状态应给出安全相关建议，实际：" + allSuggestions);
        }

        @Test
        @DisplayName("普通状态 → 给正常建议")
        void normalSafety() {
            List<CheckInRecord> records = List.of(
                    record(START, 8, 5.5, 0.25),
                    record(END, 5, 7.0, 0.75)
            );

            IndicatorResult indicators = calculator.calculate(records, SafetyLevel.NORMAL);
            StageReport report = generator.generate(indicators, START, END, USER_ID);

            assertEquals(SafetyLevel.NORMAL, report.safetyLevel());
            assertFalse(report.nextStepSuggestions().isEmpty());
        }
    }

    // ────────────────────── 纯文本报告内容验证 ──────────────────────

    @Nested
    @DisplayName("纯文本报告内容验证")
    class PlainTextValidation {

        @Test
        @DisplayName("报告可直接发微信（无特殊格式字符）")
        void wechatFriendly() {
            List<CheckInRecord> records = List.of(
                    record(START, 8, 5.5, 0.25),
                    record(START.plusDays(3), 6, 6.5, 0.5),
                    record(END, 5, 7.0, 0.75)
            );

            IndicatorResult indicators = calculator.calculate(records, SafetyLevel.NORMAL);
            String text = generator.generatePlainText(indicators, START, END);

            // 长度合理（微信单条消息建议不超过 2000 字）
            assertTrue(text.length() < 2000, "报告长度 " + text.length() + " 字，应小于 2000");
            // 不含 Markdown 格式（微信不渲染）
            assertFalse(text.contains("```"));
            assertFalse(text.contains("##"));
        }

        @Test
        @DisplayName("空数据纯文本不崩溃")
        void emptyPlainText() {
            IndicatorResult indicators = calculator.calculate(List.of(), SafetyLevel.NORMAL);

            // 空数据也能生成文本，不抛异常
            assertDoesNotThrow(() -> {
                String text = generator.generatePlainText(indicators, START, END);
                assertNotNull(text);
            });
        }

        @Test
        @DisplayName("压力上升时报告包含风险提示")
        void risingStressReport() {
            List<CheckInRecord> records = List.of(
                    record(START, 3, 7.0, 0.75),
                    record(START.plusDays(1), 5, 6.5, 0.5),
                    record(START.plusDays(2), 7, 6.0, 0.5),
                    record(END, 9, 5.0, 0.25)
            );

            IndicatorResult indicators = calculator.calculate(records, SafetyLevel.NORMAL);
            String text = generator.generatePlainText(indicators, START, END);

            assertTrue(text.contains("需要关注") || text.contains("上升"));
        }
    }
}
