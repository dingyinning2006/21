
package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

/**
 * M3模块的独立应用启动类，不依赖任何Spring服务
 */
@SpringBootApplication
public class M3StandaloneApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(M3StandaloneApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== M3模块独立模拟客户端 ===");
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
                    System.out.println("系统回复: 感谢您的语音打卡！您的打卡信息已保存。");
                } else if ("image".equalsIgnoreCase(message)) {
                    // 模拟图片消息
                    System.out.println("模拟图片消息: [图片]");
                    System.out.println("系统回复: 您好！如果您需要打卡，请直接回复打卡内容，或发送语音消息进行语音打卡。");
                } else {
                    // 处理文本消息
                    if (message.contains("打卡") || message.contains("压力") || message.contains("睡眠")) {
                        System.out.println("系统回复: 感谢您的打卡！您的打卡信息已保存。");
                    } else if (message.contains("确认") || message.contains("收到")) {
                        System.out.println("系统回复: 感谢您的确认，祝您有美好的一天！");
                    } else {
                        System.out.println("系统回复: 您好！如果您需要打卡，请直接回复打卡内容，或发送语音消息进行语音打卡。");
                    }
                }

                System.out.println("请继续输入消息，或输入 'voice'/'image'/'back':");
            }
        }

        scanner.close();
        System.out.println("模拟客户端已关闭。");
    }
}
