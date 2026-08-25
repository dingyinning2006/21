package com.example.demo.m5.model;

/**
 * 任务难度等级。用于动态重排时根据压力和完成度调整难度。
 */
public enum TaskDifficulty {

    /** 轻松：5-10分钟，几乎不需要意志力 */
    EASY("轻松", 1),

    /** 中等：15-30分钟，需要一定专注 */
    MEDIUM("中等", 2),

    /** 较难：30-45分钟，需要较强意志力和专注 */
    HARD("较难", 3);

    private final String displayName;
    private final int level;

    TaskDifficulty(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHarderThan(TaskDifficulty other) {
        return this.level > other.level;
    }

    public TaskDifficulty downgrade() {
        return switch (this) {
            case HARD -> MEDIUM;
            case MEDIUM -> EASY;
            case EASY -> EASY;
        };
    }
}
