package com.example.demo.agent.contract;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** 共享契约的轻量校验工具，集中保持各 record 的边界规则一致。 */
final class SupportContractChecks {

    private SupportContractChecks() {
    }

    static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim();
    }

    static <T> T requireNonNull(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " 不能为空");
    }

    static int requireIntRange(int value, int minInclusive, int maxInclusive, String fieldName) {
        if (value < minInclusive || value > maxInclusive) {
            throw new IllegalArgumentException(
                    fieldName + " 必须在 " + minInclusive + " 到 " + maxInclusive + " 之间"
            );
        }
        return value;
    }

    static double requireDoubleRange(double value, double minInclusive, double maxInclusive, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " 必须是有限数字");
        }

        if (value < minInclusive || value > maxInclusive) {
            throw new IllegalArgumentException(
                    fieldName + " 必须在 " + minInclusive + " 到 " + maxInclusive + " 之间"
            );
        }

        return value;
    }

    static Duration requirePositiveDuration(Duration duration, String fieldName) {
        requireNonNull(duration, fieldName);

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " 必须大于 0");
        }

        return duration;
    }

    static List<String> copyStrings(List<String> values, String fieldName) {
        requireNonNull(values, fieldName);

        return List.copyOf(values.stream()
                .map(item -> requireNonBlank(item, fieldName + " 元素"))
                .toList());
    }

    static <T> List<T> copyList(List<T> values, String fieldName) {
        requireNonNull(values, fieldName);
        return List.copyOf(values);
    }

    static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
