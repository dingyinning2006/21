
package com.example.demo.wechat;

import com.example.demo.wechat.CheckInRecord;
import com.example.demo.wechat.ReminderHandler;
import com.example.demo.wechat.WechatMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * M3模块的微信消息处理器，负责处理各种类型的微信消息
 */
@Component
public class M3MessageHandler {

    @Autowired
    private ReminderHandler reminderHandler;

    @Autowired
    private WechatMessageSender wechatMessageSender;

    /**
     * 处理文本消息
     * @param userId 用户ID
     * @param message 消息内容
     * @return 是否需要回复
     */
    public boolean handleTextMessage(String userId, String message) {
        // 检查是否是打卡消息
        if (isCheckInMessage(message)) {
            // 直接处理打卡消息，生成确认回复
            String response = "感谢您的打卡！您的打卡信息已保存。";
            wechatMessageSender.sendTextMessage(userId, response);
            return true;
        }

        // 检查是否是确认提醒的消息
        if (isReminderAckMessage(message)) {
            reminderHandler.acknowledgeReminder(userId);
            wechatMessageSender.sendTextMessage(userId, "感谢您的确认，祝您有美好的一天！");
            return true;
        }

        // 默认回复
        wechatMessageSender.sendTextMessage(userId, "您好！如果您需要打卡，请直接回复打卡内容，或发送语音消息进行语音打卡。");
        return true;
    }

    /**
     * 处理语音消息
     * @param userId 用户ID
     * @param voiceFilePath 语音文件路径
     * @return 是否需要回复
     */
    public boolean handleVoiceMessage(String userId, String voiceFilePath) {
        // 处理语音打卡
        String response = "感谢您的语音打卡！您的打卡信息已保存。";
        wechatMessageSender.sendTextMessage(userId, response);
        return true;
    }

    /**
     * 判断是否是打卡消息
     * @param message 消息内容
     * @return 是否是打卡消息
     */
    private boolean isCheckInMessage(String message) {
        // 简单判断，包含"打卡"或"压力"或"睡眠"等关键词
        return message.contains("打卡") || message.contains("压力") || message.contains("睡眠");
    }

    /**
     * 判断是否是确认提醒的消息
     * @param message 消息内容
     * @return 是否是确认提醒的消息
     */
    private boolean isReminderAckMessage(String message) {
        // 简单判断，包含"确认"或"收到"等关键词
        return message.contains("确认") || message.contains("收到");
    }
}
