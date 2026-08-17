package com.example.demo.wechat;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.listener.OnLoginListener;
import com.github.wechat.ilink.sdk.core.listener.OnMessageListener;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WeChatClient {

    private final ILinkClient client;
    private MessageHandler messageHandler;

    public WeChatClient() {
        this.client = ILinkClient.builder()
                .config(WeChatConfig.build())
                .onLogin(new OnLoginListener() {
                    @Override
                    public void onLoginSuccess(LoginContext context) {
                        System.out.println("[登录成功] botId = " + context.getBotId());
                    }

                    @Override
                    public void onLoginFailure(Throwable throwable) {
                        System.err.println("[登录失败] " + throwable.getMessage());
                    }
                })
                .onMessage(new OnMessageListener() {
                    @Override
                    public void onMessages(List<WeixinMessage> messages) {
                        if (messageHandler != null) {
                            messageHandler.handle(messages);
                        }
                    }
                })
                .build();
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public void login() throws Exception {
        String qrContent = client.executeLogin();
        QrCodeUtil.generate(qrContent, "qrcode.png");
        System.out.println("请用微信扫描项目根目录下的 qrcode.png");

        LoginContext ctx = client.getLoginFuture().get(120, TimeUnit.SECONDS);
        System.out.println("[登录完成] botId = " + ctx.getBotId());
    }

    public void sendText(String userId, String text) throws IOException {
        client.sendText(userId, text);
    }

    public void sendImage(String userId, byte[] bytes, String fileName, String caption) throws IOException{
        client.sendImage(userId, bytes, fileName, caption);
    }

    public void sendFile(String userId, byte[] bytes, String fileName, String caption) throws IOException{
        client.sendFile(userId, bytes, fileName, caption);
    }

    public List<WeixinMessage> getUpdates() throws Exception {
        return client.getUpdates();
    }

    public void close() {
        client.close();
    }
}