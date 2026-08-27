package com.example.demo.storage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.UserProfile;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** 用户连续陪伴所需的最小状态快照。 */
public record StoredSupportState(
        String userId,
        UserProfile userProfile,
        List<ScreeningResult> screenings,
        List<PlanVersion> planVersions,
        List<CheckInRecord> checkIns,
        Instant updatedAt
) {

    public StoredSupportState {
        userId = requireNonBlank(userId, "userId");
        userProfile = Objects.requireNonNull(userProfile, "userProfile 不能为空");
        if (!userId.equals(userProfile.userId())) {
            throw new IllegalArgumentException("userId 必须与 userProfile.userId 一致");
        }
        screenings = copyAndCheckUsers(userId, screenings, "screenings");
        planVersions = copyAndCheckUsers(userId, planVersions, "planVersions");
        checkIns = copyAndCheckUsers(userId, checkIns, "checkIns");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    private static <T> List<T> copyAndCheckUsers(String expectedUserId, List<T> values, String fieldName) {
        if (values == null) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        for (T value : values) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + " 不能包含 null");
            }
            String itemUserId = switch (value) {
                case ScreeningResult screening -> screening.userId();
                case PlanVersion plan -> plan.userId();
                case CheckInRecord checkIn -> checkIn.userId();
                default -> throw new IllegalArgumentException("不支持的状态类型: " + value.getClass().getSimpleName());
            };
            if (!itemUserId.equals(expectedUserId)) {
                throw new IllegalArgumentException(fieldName + " 中存在其他用户数据");
            }
        }
        return List.copyOf(values);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim();
    }
}
