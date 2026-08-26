package com.example.demo.agent.contract;

import java.time.LocalDate;
import java.util.List;

/** 7 天计划中的一天；tasks 不能为空，避免生成无行动内容的计划日。 */
public record PlanDay(
        LocalDate date,
        int dayIndex,
        List<PlanTask> tasks,
        String focus,
        boolean reducedLoad
) {

    public PlanDay {
        date = SupportContractChecks.requireNonNull(date, "date");
        dayIndex = SupportContractChecks.requireIntRange(dayIndex, 1, 7, "dayIndex");
        tasks = SupportContractChecks.copyList(tasks, "tasks");
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks 不能为空");
        }
        focus = SupportContractChecks.requireNonBlank(focus, "focus");
    }
}
