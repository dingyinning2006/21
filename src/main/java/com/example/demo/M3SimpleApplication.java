
package com.example.demo;

import com.example.demo.wechat.M3MessageHandler;
import com.example.demo.wechat.MockWechatClient;
import com.example.demo.wechat.ReminderHandler;
import com.example.demo.wechat.ReminderScheduler;
import com.example.demo.wechat.WechatMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Scanner;

/**
 * M3模块的简化应用启动类
 */
@SpringBootApplication
@EnableScheduling
public class M3SimpleApplication implements CommandLineRunner {

    @Autowired
    private M3MessageHandler m3MessageHandler;

    @Autowired
    private ReminderHandler reminderHandler;

    @Autowired
    private ReminderScheduler reminderScheduler;

    @Autowired
    private WechatMessageSender wechatMessageSender;

    public static void main(String[] args) {
        SpringApplication.run(M3SimpleApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // 初始化用户列表
        reminderScheduler.initializeUsers();

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== M3模块模拟客户端 ===");
        System.out.println("请输入用户ID (user001/user002/user003) 或输入 'exit' 退出:");

        while (true) {
            System.out.print("用户ID: ");
            String userId = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userId)) {
                break;
            }

            if (!userId.matches("user\\d{3}")) {
                System.out.println("无效用户ID，请输入 user001, user002 或 user003");
                continue;
            }

            // 添加用户到提醒列表
            reminderScheduler.addUser(userId);

            System.out.println("已选择用户: " + userId);
            System.out.println("请输入消息内容 (输入 'voice' 模拟语音消息，输入 'image' 模拟图片消息，输入 'back' 返回用户选择):");

            while (true) {
                System.out.print("消息: ");
                String message = scanner.nextLine();

                if ("back".equalsIgnoreCase(message)) {
                    break;
                }

                if ("voice".equalsIgnoreCase(message)) {
                    // 模拟语音消息
                    System.out.println("模拟语音打卡: 我今天压力很大，工作很多，睡眠不太好。");
                    m3MessageHandler.handleVoiceMessage(userId, "/tmp/voice_message.silk");
                } else if ("image".equalsIgnoreCase(message)) {
                    // 模拟图片消息
                    System.out.println("模拟图片消息: [图片]");
                    // M3模块不支持图片消息，降级为文本消息
                    m3MessageHandler.handleTextMessage(userId, "发送了一张图片");
                } else {
                    // 处理文本消息
                    m3MessageHandler.handleTextMessage(userId, message);
                }

                System.out.println("请继续输入消息，或输入 'voice'/'image'/'back':");
            }
        }

        scanner.close();
        System.out.println("模拟客户端已关闭。");
    }
}
