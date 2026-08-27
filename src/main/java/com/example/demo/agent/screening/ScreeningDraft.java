package com.example.demo.agent.screening;

import java.util.List;

public record ScreeningDraft(
        String userId,
        String displayName,
        List<String> stressSources,
        Integer stressLevel,
        Boolean sleepAffected,
        Boolean functionImpaired
) {
    public ScreeningDraft {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName 不能为空");
        }

        stressSources = stressSources == null
                ? List.of()
                : List.copyOf(stressSources);
    }
}