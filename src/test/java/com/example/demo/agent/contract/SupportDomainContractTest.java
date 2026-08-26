package com.example.demo.agent.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 用固定 fixture 验证跨模块共用的字段、JSON 和安全边界。 */
class SupportDomainContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void supportsNormalCompanionshipRoundTrip() throws Exception {
        UserProfile userProfile = new UserProfile(
                "u-1001",
                "小林",
                List.of("求职", "失眠"),
                LocalDate.of(2026, 8, 25),
                SupportPhase.SCREENING,
                SafetyLevel.NORMAL
        );

        ScreeningResult screeningResult = new ScreeningResult(
                "u-1001",
                LocalDate.of(2026, 8, 25),
                7,
                true,
                true,
                List.of("毕业", "面试"),
                "求职压力",
                SupportPhase.PLANNING
        );

        PlanTask task = new PlanTask(
                "task-1",
                "整理面试问题",
                "现实任务",
                "把最近 3 次面试中最常被问到的问题整理出来。",
                Duration.ofMinutes(20),
                TaskStatus.TODO,
                "列出不少于 5 个问题并完成简短回答",
                "只整理 3 个高频问题"
        );

        PlanDay planDay = new PlanDay(
                LocalDate.of(2026, 8, 26),
                1,
                List.of(task),
                "稳住节奏",
                false
        );

        CheckInRecord checkInRecord = new CheckInRecord(
                "u-1001",
                LocalDate.of(2026, 8, 26),
                6,
                6.5,
                6,
                0.5,
                List.of("task-1"),
                List.of(),
                "今天先完成一个最小动作",
                SupportPhase.CHECK_IN
        );

        SafetyDecision safetyDecision = new SafetyDecision(
                "u-1001",
                SafetyLevel.NORMAL,
                false,
                List.of(),
                "先陪你把压力拆开，我们一步一步来。",
                "当前没有高危信号"
        );

        SupportSession session = new SupportSession(
                "session-1",
                userProfile,
                SupportPhase.PLAN_ACTIVE,
                SafetyLevel.NORMAL,
                screeningResult,
                safetyDecision,
                List.of(planDay),
                List.of(checkInRecord),
                Instant.parse("2026-08-25T10:00:00Z"),
                Instant.parse("2026-08-25T10:15:00Z")
        );

        String json = objectMapper.writeValueAsString(session);
        SupportSession restored = objectMapper.readValue(json, SupportSession.class);

        assertEquals("session-1", restored.sessionId());
        assertEquals(SupportPhase.PLAN_ACTIVE, restored.phase());
        assertEquals(1, restored.planDays().size());
        assertEquals("小林", restored.userProfile().displayName());
    }

    @Test
    void rejectsInvalidStressLevel() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ScreeningResult(
                        "u-1001",
                        LocalDate.of(2026, 8, 25),
                        11,
                        true,
                        true,
                        List.of("毕业"),
                        "求职压力",
                        SupportPhase.PLANNING
                )
        );

        assertTrue(exception.getMessage().contains("stressLevel"));
    }

    @Test
    void rejectsInvalidSleepHours() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new CheckInRecord(
                        "u-1001",
                        LocalDate.of(2026, 8, 25),
                        5,
                        25,
                        6,
                        0.5,
                        List.of(),
                        List.of(),
                        "",
                        SupportPhase.CHECK_IN
                )
        );

        assertTrue(exception.getMessage().contains("sleepHours"));
    }

    @Test
    void rejectsInvalidCompletionRate() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new StageReport(
                        "u-1001",
                        LocalDate.of(2026, 8, 19),
                        LocalDate.of(2026, 8, 25),
                        SafetyLevel.NORMAL,
                        -0.5,
                        0.2,
                        1.2,
                        List.of("压力下降"),
                        List.of("继续保持固定作息"),
                        "阶段总结"
                )
        );

        assertTrue(exception.getMessage().contains("completionRate"));
    }

    @Test
    void rejectsUrgentDecisionWithoutNormalChatStop() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new SafetyDecision(
                        "u-1001",
                        SafetyLevel.URGENT,
                        false,
                        List.of("联系身边可信任的人"),
                        "请立刻联系身边可信任的人。",
                        "命中高危表达"
                )
        );

        assertTrue(exception.getMessage().contains("URGENT"));
    }

    @Test
    void supportsUrgentSafetyFlowFixture() {
        SafetyDecision safetyDecision = new SafetyDecision(
                "u-urgent",
                SafetyLevel.URGENT,
                true,
                List.of("联系身边可信任的人", "联系当地急救或危机热线", "联系专业心理咨询师或医院"),
                "我现在先陪你联系身边可信任的人，请先不要独处。",
                "命中需要立即人工支持的高危表达"
        );

        SupportSession session = new SupportSession(
                "session-urgent",
                new UserProfile(
                        "u-urgent",
                        "小李",
                        List.of("求职压力"),
                        LocalDate.of(2026, 8, 25),
                        SupportPhase.SAFETY_FLOW,
                        SafetyLevel.URGENT
                ),
                SupportPhase.SAFETY_FLOW,
                SafetyLevel.URGENT,
                null,
                safetyDecision,
                List.of(),
                List.of(),
                Instant.parse("2026-08-25T10:00:00Z"),
                Instant.parse("2026-08-25T10:00:00Z")
        );

        assertTrue(session.safetyDecision().stopNormalChat());
        assertEquals(SupportPhase.SAFETY_FLOW, session.phase());
    }

    @Test
    void supportsUnfinishedTaskFixture() {
        CheckInRecord checkInRecord = new CheckInRecord(
                "u-1003",
                LocalDate.of(2026, 8, 26),
                8,
                5.5,
                4,
                0,
                List.of(),
                List.of("task-interview-1"),
                "今天状态比较差，只完成了必要休息。",
                SupportPhase.CHECK_IN
        );

        assertEquals(0, checkInRecord.completionRate());
        assertEquals(List.of("task-interview-1"), checkInRecord.missedTaskIds());
    }

    @Test
    void supportsVoiceRecognitionFailureFixture() throws Exception {
        CheckInFailure failure = new CheckInFailure(
                "u-1004",
                LocalDate.of(2026, 8, 26),
                CheckInFailureCode.VOICE_TRANSCRIPTION_FAILED,
                "这段语音我没有识别清楚，可以再说一遍或改用文字打卡。",
                true
        );

        CheckInFailure restored = objectMapper.readValue(
                objectMapper.writeValueAsString(failure),
                CheckInFailure.class
        );

        assertEquals(CheckInFailureCode.VOICE_TRANSCRIPTION_FAILED, restored.code());
        assertTrue(restored.retryable());
    }

    @Test
    void rejectsPlanDayOutsideSevenDays() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new PlanDay(
                        LocalDate.of(2026, 8, 25),
                        8,
                        List.of(new PlanTask(
                                "task-1",
                                "整理资料",
                                "现实任务",
                                "准备一页资料",
                                Duration.ofMinutes(15),
                                TaskStatus.TODO,
                                "完成一页清单",
                                null
                        )),
                        "收尾",
                        false
                )
        );

        assertTrue(exception.getMessage().contains("dayIndex"));
    }

    @Test
    void keepsListsImmutable() {
        List<String> stressSources = new ArrayList<>(List.of("毕业", "面试"));
        UserProfile profile = new UserProfile(
                "u-1002",
                "小王",
                stressSources,
                LocalDate.of(2026, 8, 25),
                SupportPhase.LISTENING,
                SafetyLevel.NORMAL
        );

        stressSources.add("家人");

        assertEquals(2, profile.stressSources().size());
        assertThrows(UnsupportedOperationException.class, () -> profile.stressSources().add("新压力源"));
    }
}
