
package com.example.demo;

import com.example.demo.wechat.MockWechatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 微信语音陪伴与每日打卡模拟应用启动类
 */
@SpringBootApplication
@EnableScheduling
public class MockWechatApplication implements CommandLineRunner {

    @Autowired
    private MockWechatClient mockWechatClient;

    public static void main(String[] args) {
        SpringApplication.run(MockWechatApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // 启动模拟客户端
        mockWechatClient.start();
    }
}
