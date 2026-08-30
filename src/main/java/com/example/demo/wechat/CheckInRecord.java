
package com.example.demo.wechat;

import java.time.LocalDateTime;

/**
 * 打卡记录契约，用于统一文字打卡和语音打卡的数据结构
 */
public class CheckInRecord {
    private String userId;
    private LocalDateTime checkInTime;
    private String pressureLevel; // 压力水平：低、中、高
    private String sleepQuality; // 睡眠质量：好、一般、差
    private String completedTasks; // 已完成任务，逗号分隔
    private String difficulties; // 遇到的困难
    private String nextDayPlan; // 次日计划
    private String note; // 备注

    // 构造方法
    public CheckInRecord() {}

    public CheckInRecord(String userId) {
        this.userId = userId;
        this.checkInTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getPressureLevel() {
        return pressureLevel;
    }

    public void setPressureLevel(String pressureLevel) {
        this.pressureLevel = pressureLevel;
    }

    public String getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(String sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public String getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(String completedTasks) {
        this.completedTasks = completedTasks;
    }

    public String getDifficulties() {
        return difficulties;
    }

    public void setDifficulties(String difficulties) {
        this.difficulties = difficulties;
    }

    public String getNextDayPlan() {
        return nextDayPlan;
    }

    public void setNextDayPlan(String nextDayPlan) {
        this.nextDayPlan = nextDayPlan;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * 检查打卡是否完整
     */
    public boolean isComplete() {
        return pressureLevel != null && sleepQuality != null && 
               completedTasks != null && difficulties != null;
    }

    /**
     * 获取缺失的字段
     */
    public String getMissingFields() {
        StringBuilder missing = new StringBuilder();

        if (pressureLevel == null) {
            missing.append("压力水平, ");
        }
        if (sleepQuality == null) {
            missing.append("睡眠质量, ");
        }
        if (completedTasks == null) {
            missing.append("已完成任务, ");
        }
        if (difficulties == null) {
            missing.append("遇到的困难, ");
        }

        if (missing.length() > 0) {
            missing.setLength(missing.length() - 2); // 移除最后的逗号和空格
        }

        return missing.toString();
    }
}
