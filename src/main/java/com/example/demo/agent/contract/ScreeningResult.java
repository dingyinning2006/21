package com.example.demo.agent.contract;

import java.time.LocalDate;
import java.util.List;

/** 一次压力初筛的结果；不用于精神疾病诊断。 */
public record ScreeningResult(
        String userId,
        LocalDate recordedOn,
        int stressLevel, // 主观压力强度，范围 0-10
        boolean sleepAffected,
        boolean functionImpaired,
        List<String> stressSources,
        String mainStressor,
        SupportPhase nextPhase
) {

    public ScreeningResult {
        userId = SupportContractChecks.requireNonBlank(userId, "userId");
        recordedOn = SupportContractChecks.requireNonNull(recordedOn, "recordedOn");
        stressLevel = SupportContractChecks.requireIntRange(stressLevel, 0, 10, "stressLevel");
        stressSources = SupportContractChecks.copyStrings(stressSources, "stressSources");
        mainStressor = SupportContractChecks.requireNonBlank(mainStressor, "mainStressor");
        nextPhase = SupportContractChecks.requireNonNull(nextPhase, "nextPhase");
    }
}
