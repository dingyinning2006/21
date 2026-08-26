package com.example.demo.agent.contract;

/** 计划任务状态；REDUCED 表示任务已切换到缩小版本。 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    SKIPPED,
    REDUCED
}
