package com.example.demo.agent.safety;

import com.example.demo.agent.contract.SafetyDecision;
import com.example.demo.agent.contract.SafetyLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyRouterTest {

    private final SafetyRouter safetyRouter = new SafetyRouter();

    @Test
    void keepsNormalConversationForOrdinaryStress() {
        SafetyDecision decision = safetyRouter.evaluate(
                "user-101",
                "我最近因为毕业和求职很焦虑"
        );

        assertEquals(SafetyLevel.NORMAL, decision.level());
        assertFalse(decision.stopNormalChat());
        assertTrue(decision.actions().isEmpty());
    }

    @Test
    void routesUrgentSignalToSafetyFlow() {
        SafetyDecision decision = safetyRouter.evaluate(
                "user-102",
                "我已经有自杀计划，不想活了"
        );

        assertEquals(SafetyLevel.URGENT, decision.level());
        assertTrue(decision.stopNormalChat());
        assertEquals(3, decision.actions().size());
        assertTrue(decision.actions().get(0).contains("可信任的人"));
        assertTrue(decision.actions().get(1).contains("当地急救或危机热线"));
        assertTrue(decision.actions().get(2).contains("专业心理咨询师"));
        assertTrue(decision.rationale().contains("高危表达信号"));
    }

    @Test
    void treatsBlankMessageAsNormalWithoutThrowing() {
        SafetyDecision decision = safetyRouter.evaluate("user-103", " ");

        assertEquals(SafetyLevel.NORMAL, decision.level());
        assertFalse(decision.stopNormalChat());
    }

    @Test
    void keepsUserInSafetyFlowAfterUrgentSignal() {
        safetyRouter.evaluate("user-104", "我不想活了");

        SafetyDecision followUp = safetyRouter.evaluate(
                "user-104",
                "我想继续聊求职计划"
        );

        assertEquals(SafetyLevel.URGENT, followUp.level());
        assertTrue(followUp.stopNormalChat());
        assertTrue(followUp.rationale().contains("安全流程"));
    }

    @Test
    void explainsWhenCrisisHotlineIsNotConfigured() {
        SafetyDecision decision = safetyRouter.evaluate("user-105", "我准备去死");

        assertTrue(decision.actions().stream()
                .anyMatch(action -> action.contains("尚未配置") && action.contains("人工")));
    }
}
