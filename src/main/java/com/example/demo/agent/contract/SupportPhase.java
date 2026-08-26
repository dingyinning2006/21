package com.example.demo.agent.contract;

/** Agent 的陪伴阶段，模块通过它判断下一步允许执行的动作。 */
public enum SupportPhase {
    LISTENING,
    SCREENING,
    PLANNING,
    PLAN_ACTIVE,
    CHECK_IN,
    REVIEW,
    SAFETY_FLOW
}
