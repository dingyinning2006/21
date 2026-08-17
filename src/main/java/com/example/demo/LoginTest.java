package com.example.demo;

import com.example.demo.wechat.WeChatClient;

public class LoginTest {
    public static void main(String[] args) throws Exception {
        WeChatClient client = new WeChatClient();
        client.login();
        // 登录成功后不要让程序退出，保持长连接
        Thread.currentThread().join();
    }
}
