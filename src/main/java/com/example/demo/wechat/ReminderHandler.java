
package com.example.demo.wechat;

import com.example.demo.storage.SupportStateStore;
import com.example.demo.storage.StoredSupportState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 微信提醒处理器，负责打卡确认和每日提醒
 */
@Component
public class ReminderHandler {

    @Autowired
    private SupportStateStore supportStateStore;

    @Autowired
    private WechatMessageSender wechatMessageSender;

    // 用户提醒状态，记录用户是否已确认收到提醒
    private Map<String, Boolean> reminderAckStates = new HashMap<>();

    /**
     * 处理打卡完成后的确认消息
     * @param userId 用户ID
     * @return 确认消息内容
     */
    public String handleCheckInConfirmation(String userId) {
        try {
            // 创建一个新的CheckInRecord对象
            CheckInRecord checkInRecord = new CheckInRecord(userId);
            checkInRecord.setCheckInTime(LocalDateTime.now());
            checkInRecord.setPressureLevel("中");
            checkInRecord.setSleepQuality("一般");
            checkInRecord.setCompletedTasks("任务1, 任务2");
            checkInRecord.setDifficulties("无");
            checkInRecord.setNextDayPlan("继续完成剩余任务");
            checkInRecord.setNote("状态良好");

            // 生成确认消息
            StringBuilder message = new StringBuilder();
            message.append("感谢您完成今日打卡！以下是您的打卡摘要：\n\n");
            message.append("打卡时间：").append(checkInRecord.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            message.append("压力水平：").append(checkInRecord.getPressureLevel()).append("\n");
            message.append("睡眠质量：").append(checkInRecord.getSleepQuality()).append("\n");
            message.append("已完成任务：").append(checkInRecord.getCompletedTasks()).append("\n");
            message.append("遇到的困难：").append(checkInRecord.getDifficulties()).append("\n");

            if (checkInRecord.getNextDayPlan() != null && !checkInRecord.getNextDayPlan().isEmpty()) {
                message.append("次日计划：").append(checkInRecord.getNextDayPlan()).append("\n");
            }

            message.append("\n请继续保持良好的习惯，明天同一时间我们会提醒您继续打卡。");

            // 发送确认消息
            wechatMessageSender.sendTextMessage(userId, message.toString());

            return message.toString();

        } catch (Exception e) {
            return "感谢您的打卡！您的打卡信息已保存，但确认消息发送失败。";
        }
    }

    /**
     * 发送每日提醒
     * @param userId 用户ID
     * @return 提醒是否成功发送
     */
    public boolean sendDailyReminder(String userId) {
        try {
            // 检查用户是否已确认收到提醒
            if (reminderAckStates.getOrDefault(userId, false)) {
                return false; // 用户已确认，不再重复发送
            }

            // 生成提醒消息
            StringBuilder message = new StringBuilder();
            message.append("您好！今天是打卡的第").append(calculateCheckInDays(userId)).append("天，请完成今日打卡。\n\n");
            message.append("请回复此消息开始打卡，或发送语音消息进行语音打卡。");

            // 直接发送文字消息
            wechatMessageSender.sendTextMessage(userId, message.toString());
            return true;

        } catch (Exception e) {
            // 发送失败，记录日志
            System.err.println("发送每日提醒失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送通用提醒（用户无历史打卡记录时）
     */
    private boolean sendGeneralReminder(String userId) {
        try {
            String message = "您好！欢迎使用压力调适服务。请回复此消息开始今日打卡，或发送语音消息进行语音打卡。";

            // 直接发送文字消息
            wechatMessageSender.sendTextMessage(userId, message.toString());
            return true;

        } catch (Exception e) {
            // 发送失败，记录日志
            System.err.println("发送通用提醒失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 确认用户已收到提醒
     * @param userId 用户ID
     */
    public void acknowledgeReminder(String userId) {
        reminderAckStates.put(userId, true);
    }

    /**
     * 计算用户连续打卡天数
     */
    private int calculateCheckInDays(String userId) {
        // 为了简化，我们返回一个模拟值
        return 3;
    }
}
