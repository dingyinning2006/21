package com.example.demo.agent.planning;

import com.example.demo.agent.contract.CheckInRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalCheckInParserTest {

    private final LocalCheckInParser parser = new LocalCheckInParser();

    @Test
    void parsesStructuredChineseCheckIn() {
        CheckInRecord record = parser.parse(
                        "parser-user",
                        LocalDate.of(2026, 8, 27),
                        "今日打卡：压力 8 分，睡眠 5.5 小时，心情 4 分，任务完成率 50%"
                )
                .orElseThrow();

        assertEquals(8, record.stressLevel());
        assertEquals(5.5, record.sleepHours());
        assertEquals(4, record.moodLevel());
        assertEquals(0.5, record.completionRate());
    }

    @Test
    void doesNotGuessWhenCheckInFieldIsMissing() {
        assertTrue(parser.parse(
                "parser-user",
                LocalDate.of(2026, 8, 27),
                "今日打卡：压力 8 分，睡眠 5 小时，任务完成率 50%"
        ).isEmpty());
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertTrue(parser.parse(
                "parser-user",
                LocalDate.of(2026, 8, 27),
                "今日打卡：压力 11 分，睡了 5 个小时，心情 4 分，任务完成率 50%"
        ).isEmpty());
    }
}
