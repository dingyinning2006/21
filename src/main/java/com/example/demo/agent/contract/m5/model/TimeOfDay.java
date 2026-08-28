package com.example.demo.agent.contract.m5.model;

/**
 * 任务建议执行时段。
 */
public enum TimeOfDay {

    /** 晨间：起床后-上午，适合启动性任务 */
    MORNING("晨间"),

    /** 日间：上午-傍晚，适合专注型任务 */
    DAYTIME("日间"),

    /** 睡前：睡前30分钟内，仅限低唤醒放松任务 */
    BEDTIME("睡前");

    private final String displayName;

    TimeOfDay(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
