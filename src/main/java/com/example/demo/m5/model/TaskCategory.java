package com.example.demo.m5.model;

/**
 * 现实任务类别。
 * 对应 M5-001 要求的五类模板：面试准备、考试复习、拖延启动、人际沟通、睡前低唤醒。
 */
public enum TaskCategory {

    /** 求职面试准备 */
    INTERVIEW_PREP("面试准备", "求职压力"),

    /** 考试复习 */
    EXAM_REVIEW("考试复习", "考试焦虑"),

    /** 拖延启动 */
    PROCRASTINATION_START("拖延启动", "拖延行为"),

    /** 人际沟通 */
    INTERPERSONAL("人际沟通", "人际压力"),

    /** 睡前低唤醒放松 */
    BEDTIME_RELAXATION("睡前放松", "睡眠紊乱");

    private final String displayName;
    private final String stressSource;

    TaskCategory(String displayName, String stressSource) {
        this.displayName = displayName;
        this.stressSource = stressSource;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStressSource() {
        return stressSource;
    }
}
