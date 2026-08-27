package com.example.demo.agent.contract;

import java.time.LocalDate;
import java.util.List;

/** 用户的长期基础状态，不保存一次具体打卡的明细。 */
public record UserProfile(
        String userId,
        String displayName,
        List<String> stressSources,
        LocalDate joinedOn,
        SupportPhase supportPhase,
        SafetyLevel safetyLevel
) {

    public UserProfile {
        userId = SupportContractChecks.requireNonBlank(userId, "userId");
        displayName = SupportContractChecks.requireNonBlank(displayName, "displayName");
        stressSources = SupportContractChecks.copyStrings(stressSources, "stressSources");
        joinedOn = SupportContractChecks.requireNonNull(joinedOn, "joinedOn");
        supportPhase = SupportContractChecks.requireNonNull(supportPhase, "supportPhase");
        safetyLevel = SupportContractChecks.requireNonNull(safetyLevel, "safetyLevel");
    }
}
