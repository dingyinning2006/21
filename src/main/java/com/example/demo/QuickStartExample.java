package com.example.demo;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.listener.OnLoginListener;
import com.github.wechat.ilink.sdk.core.listener.OnMessageListener;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class QuickStartExample {

    public static void main(String[] args) throws Exception {
        ILinkConfig config = ILinkConfig.builder()
                .connectTimeoutMs(35000)
                .readTimeoutMs(35000)
                .writeTimeoutMs(35000)
                .httpMaxRetries(3)
                .retryBaseDelayMs(1000)
                .retryMaxDelayMs(10000)
                .heartbeatEnabled(true)
                .heartbeatIntervalMs(30000)
                .channelVersion("1.0.0")
                .build();

        ILinkClient client = ILinkClient.builder()
                .config(config)
                .onLogin(new OnLoginListener() {
                    @Override
                    public void onLoginSuccess(LoginContext context) {
                        System.out.println("登录成功，botId = " + context.getBotId());
                    }

                    @Override
                    public void onLoginFailure(Throwable throwable) {
                        System.err.println("登录失败: " + throwable.getMessage());
                    }
                })
                .onMessage(new OnMessageListener() {
                    @Override
                    public void onMessages(List<WeixinMessage> messages) {
                        for (WeixinMessage msg : messages) {
                            System.out.println("收到消息 fromUserId = " + msg.getFrom_user_id());
                            if (msg.getItem_list() != null) {
                                for (MessageItem item : msg.getItem_list()) {
                                    if (item.getText_item() != null) {
                                        System.out.println("text = " + item.getText_item().getText());
                                    }
                                }
                            }
                        }
                    }
                })
                .build();

        try {
            String qrCodeContent = client.executeLogin();
            System.out.println("请扫码登录：");
            System.out.println(qrCodeContent);

            LoginContext context = client.getLoginFuture().get();
            System.out.println("登录完成，botId = " + context.getBotId());

            List<WeixinMessage> messages = client.getUpdates();
            System.out.println("首次拉取消息数 = " + messages.size());

            String targetUserId = "这里替换成真实的 from_user_id";

            client.sendText(targetUserId, "Hello, iLink!");
            client.sendTextWithTyping(targetUserId, "这是一条带输入态的消息", 1500L);

            byte[] imageBytes = Files.readAllBytes(Paths.get("demo.png"));
            client.sendImage(targetUserId, imageBytes, "demo.png", "这是一张测试图片");

            byte[] fileBytes = Files.readAllBytes(Paths.get("demo.pdf"));
            client.sendFile(targetUserId, fileBytes, "demo.pdf", "这是一个测试文件");

            byte[] voiceBytes = Files.readAllBytes(Paths.get("demo.silk"));
            client.sendVoice(targetUserId, voiceBytes, "demo.silk", 3000, 16000);

            byte[] videoBytes = Files.readAllBytes(Paths.get("demo.mp4"));
            client.sendVideo(targetUserId, videoBytes, "demo.mp4", 5000, "这是一个测试视频");

        } finally {
            client.close();
        }
    }
}