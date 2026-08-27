package com.example.demo.agent.contract;

import java.util.List;

/** 安全路由模块的决定；停止普通聊天时必须同时给出安全行动。 */
public record SafetyDecision(
        String userId,
        SafetyLevel level,
        boolean stopNormalChat,
        List<String> actions,
        String message,
        String rationale
) {

    public SafetyDecision {
        userId = SupportContractChecks.requireNonBlank(userId, "userId");
        level = SupportContractChecks.requireNonNull(level, "level");
        actions = SupportContractChecks.copyStrings(actions, "actions");
        message = SupportContractChecks.requireNonBlank(message, "message");
        rationale = SupportContractChecks.requireNonBlank(rationale, "rationale");

        if (level == SafetyLevel.URGENT && !stopNormalChat) {
            throw new IllegalArgumentException("URGENT 安全决策必须停止普通聊天");
        }

        if (level == SafetyLevel.NORMAL && stopNormalChat) {
            throw new IllegalArgumentException("NORMAL 安全决策不能停止普通聊天");
        }

        if (stopNormalChat && actions.isEmpty()) {
            throw new IllegalArgumentException("停止普通聊天时必须提供安全行动");
        }
    }
}
