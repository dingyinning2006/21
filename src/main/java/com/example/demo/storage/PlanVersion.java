package com.example.demo.storage;

import com.example.demo.agent.contract.PlanDay;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 一次 7 天游程版本；同一用户可保留多个版本用于比较调整结果。 */
public record PlanVersion(
        String userId,
        String versionId,
        List<PlanDay> planDays,
        Instant createdAt
) {

    public PlanVersion {
        userId = requireNonBlank(userId, "userId");
        versionId = requireNonBlank(versionId, "versionId");
        if (planDays == null || planDays.isEmpty()) {
            throw new IllegalArgumentException("planDays 不能为空");
        }
        planDays = List.copyOf(planDays);
        createdAt = java.util.Objects.requireNonNull(createdAt, "createdAt 不能为空");

        Set<LocalDate> dates = new HashSet<>();
        Set<Integer> dayIndexes = new HashSet<>();
        for (PlanDay planDay : planDays) {
            if (!dates.add(planDay.date())) {
                throw new IllegalArgumentException("planDays 不能包含重复日期");
            }
            if (!dayIndexes.add(planDay.dayIndex())) {
                throw new IllegalArgumentException("planDays 不能包含重复 dayIndex");
            }
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim();
    }
}
