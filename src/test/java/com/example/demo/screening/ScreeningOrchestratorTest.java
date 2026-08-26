package com.example.demo.screening;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.SupportPhase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.demo.agent.screening.ScreeningOrchestrator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreeningOrchestratorTest {

    @Test
    void asksForStressLevelWhenUserOnlyDescribesStress() {
        ScreeningOrchestrator orchestrator = new ScreeningOrchestrator();

        String reply = orchestrator.handleFirstMessage(
                "user-001",
                "小林",
                "我最近因为毕业和求职压力很焦虑"
        );

        assertTrue(reply.contains("毕业压力"));
        assertTrue(reply.contains("求职压力"));
        assertTrue(reply.contains("0-10"));
    }
    @Test
    void continuesScreeningWhenStressLevelIsProvided() {
        ScreeningOrchestrator orchestrator = new ScreeningOrchestrator();

        String reply = orchestrator.handleFirstMessage(
                "user-002",
                "小王",
                "我最近因为面试很焦虑，压力大概是 8 分，最近睡眠没有受到影响"
        );

        assertTrue(reply.contains("8 分"));
        assertTrue(reply.contains("睡眠"));
        assertTrue(reply.contains("学习、求职或日常生活"));
    }

    @Test
    void keepsStressLevelAcrossMultipleMessages() {
        ScreeningOrchestrator orchestrator = new ScreeningOrchestrator();

        orchestrator.handleFirstMessage(
                "user-004",
                "小赵",
                "我最近因为毕业和求职很焦虑"
        );

        String secondReply = orchestrator.handleFirstMessage(
                "user-004",
                "小赵",
                "压力大概是 8 分"
        );

        assertTrue(secondReply.contains("8 分"));
        assertTrue(secondReply.contains("睡眠"));
        assertTrue(!secondReply.contains("0-10"));
    }

    @Test
    void completesScreeningAfterMultipleAnswers() {
        ScreeningOrchestrator orchestrator = new ScreeningOrchestrator();

        orchestrator.handleFirstMessage(
                "user-005",
                "小陈",
                "我最近因为毕业和面试很焦虑"
        );

        orchestrator.handleFirstMessage(
                "user-005",
                "小陈",
                "压力大概是 8 分"
        );

        orchestrator.handleFirstMessage(
                "user-005",
                "小陈",
                "最近经常失眠"
        );

        String finalReply = orchestrator.handleFirstMessage(
                "user-005",
                "小陈",
                "已经学不进去了"
        );

        assertTrue(finalReply.contains("初筛已经完成"));
        assertTrue(finalReply.contains("8 分"));
        assertTrue(finalReply.contains("毕业压力"));
        assertTrue(finalReply.contains("求职压力"));
        assertTrue(finalReply.contains("计划制定阶段"));
    }

    @Test
    void buildsScreeningResultAfterRequiredAnswersAreCollected() {
        ScreeningOrchestrator orchestrator = new ScreeningOrchestrator();

        ScreeningResult result = orchestrator.buildScreeningResult(
                "user-003",
                8,
                true,
                true,
                List.of("毕业压力", "求职压力")
        );

        assertEquals("user-003", result.userId());
        assertEquals(8, result.stressLevel());
        assertEquals(true, result.sleepAffected());
        assertEquals(true, result.functionImpaired());
        assertEquals(List.of("毕业压力", "求职压力"), result.stressSources());
        assertEquals("毕业压力、求职压力", result.mainStressor());
        assertEquals(SupportPhase.PLANNING, result.nextPhase());
    }
}
