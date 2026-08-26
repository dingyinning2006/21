package com.example.demo.agent.contract;

import java.time.Instant;
import java.util.List;

/** 当前 Agent 会话的聚合状态，是编排、存储和报告模块的共同入口。 */
public record SupportSession(
        String sessionId,
        UserProfile userProfile,
        SupportPhase phase,
        SafetyLevel safetyLevel,
        ScreeningResult screeningResult,
        SafetyDecision safetyDecision,
        List<PlanDay> planDays,
        List<CheckInRecord> checkInRecords,
        Instant createdAt,
        Instant updatedAt
) {

    public SupportSession {
        sessionId = SupportContractChecks.requireNonBlank(sessionId, "sessionId");
        userProfile = SupportContractChecks.requireNonNull(userProfile, "userProfile");
        phase = SupportContractChecks.requireNonNull(phase, "phase");
        safetyLevel = SupportContractChecks.requireNonNull(safetyLevel, "safetyLevel");
        planDays = SupportContractChecks.copyList(planDays, "planDays");
        checkInRecords = SupportContractChecks.copyList(checkInRecords, "checkInRecords");
        createdAt = SupportContractChecks.requireNonNull(createdAt, "createdAt");
        updatedAt = SupportContractChecks.requireNonNull(updatedAt, "updatedAt");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt 不能早于 createdAt");
        }

        if (screeningResult != null && !userProfile.userId().equals(screeningResult.userId())) {
            throw new IllegalArgumentException("screeningResult.userId 必须与 userProfile.userId 一致");
        }

        if (safetyDecision != null && !userProfile.userId().equals(safetyDecision.userId())) {
            throw new IllegalArgumentException("safetyDecision.userId 必须与 userProfile.userId 一致");
        }

        // 高危状态禁止继续走普通计划和打卡流程。
        if (safetyLevel == SafetyLevel.URGENT && phase != SupportPhase.SAFETY_FLOW) {
            throw new IllegalArgumentException("URGENT 安全状态必须进入 SAFETY_FLOW");
        }

        if (safetyDecision != null && safetyDecision.stopNormalChat() && phase != SupportPhase.SAFETY_FLOW) {
            throw new IllegalArgumentException("停止普通聊天时会话阶段必须是 SAFETY_FLOW");
        }
    }
}
