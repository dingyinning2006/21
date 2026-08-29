
package com.example.demo.wechat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 提醒调度器，负责定时发送每日提醒
 */
@Component
public class ReminderScheduler {

    @Autowired
    private ReminderHandler reminderHandler;

    @Autowired
    private WechatMessageSender wechatMessageSender;

    // 用户列表，实际应用中应该从数据库获取
    private List<String> userList = new CopyOnWriteArrayList<>();

    /**
     * 初始化用户列表（模拟）
     */
    public void initializeUsers() {
        // 模拟一些用户
        userList.add("user001");
        userList.add("user002");
        userList.add("user003");
    }

    /**
     * 添加用户到提醒列表
     * @param userId 用户ID
     */
    public void addUser(String userId) {
        if (!userList.contains(userId)) {
            userList.add(userId);
        }
    }

    /**
     * 从提醒列表移除用户
     * @param userId 用户ID
     */
    public void removeUser(String userId) {
        userList.remove(userId);
    }

    /**
     * 每日定时提醒任务，早上9点执行
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void dailyReminderTask() {
        System.out.println("执行每日提醒任务: " + LocalTime.now());

        for (String userId : userList) {
            boolean success = reminderHandler.sendDailyReminder(userId);
            if (!success) {
                System.err.println("发送提醒失败给用户: " + userId);
            }
        }
    }

    /**
     * 每日打卡确认任务，晚上9点执行
     */
    @Scheduled(cron = "0 0 21 * * ?")
    public void dailyConfirmationTask() {
        System.out.println("执行每日打卡确认任务: " + LocalTime.now());

        for (String userId : userList) {
            String confirmation = reminderHandler.handleCheckInConfirmation(userId);
            // 确认消息已经由ReminderHandler内部发送，这里只是记录
            System.out.println("用户 " + userId + " 的打卡确认消息: " + confirmation);
        }
    }
}
