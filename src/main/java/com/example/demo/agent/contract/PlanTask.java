package com.example.demo.agent.contract;

import java.time.Duration;

/** 一项可在当天完成的现实任务或调适任务。 */
public record PlanTask(
        String taskId,
        String title,
        String category,
        String description,
        Duration estimatedDuration,
        TaskStatus status,
        String successCriteria,
        String fallbackTaskId
) {

    public PlanTask {
        taskId = SupportContractChecks.requireNonBlank(taskId, "taskId");
        title = SupportContractChecks.requireNonBlank(title, "title");
        category = SupportContractChecks.requireNonBlank(category, "category");
        description = SupportContractChecks.requireNonBlank(description, "description");
        estimatedDuration = SupportContractChecks.requirePositiveDuration(estimatedDuration, "estimatedDuration");
        status = SupportContractChecks.requireNonNull(status, "status");
        successCriteria = SupportContractChecks.requireNonBlank(successCriteria, "successCriteria");
        fallbackTaskId = SupportContractChecks.normalizeOptional(fallbackTaskId);
    }
}
