package com.example.demo.bot;

import com.example.demo.bot.handler.WechatMessageHandler;
import com.example.demo.config.WechatSessionStore;
import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 微信连接主循环。
 *
 * 这里只负责登录、恢复会话和拉取消息，具体业务交给 WechatMessageHandler。
 */
@Component
public class WechatLoginRunner implements ApplicationRunner {

    private final ILinkClient client;
    private final WechatSessionStore sessionStore;
    private final WechatMessageHandler messageHandler;

    public WechatLoginRunner(
            ILinkClient client,
            WechatSessionStore sessionStore,
            WechatMessageHandler messageHandler
    ) {
        this.client = client;
        this.sessionStore = sessionStore;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LoginContext context = loginOrResume();

        System.out.println("微信机器人已启动，botId = " + context.getBotId());
        messageLoop();
    }

    /**
     * 优先使用 SDK 中已有的登录上下文，没有时执行扫码登录并保存会话。
     */
    private LoginContext loginOrResume() throws Exception {
        LoginContext context = client.getLoginContext();

        if (context != null) {
            System.out.println("已恢复微信登录，botId = " + context.getBotId());
            return context;
        }

        String qrCodeContent = client.executeLogin();
        System.out.println("请将下面内容生成二维码，然后用手机微信扫码登录：");
        System.out.println(qrCodeContent);

        context = client.getLoginFuture().get();
        sessionStore.save(context);
        System.out.println("登录成功，botId = " + context.getBotId());
        return context;
    }

    /**
     * 持续拉取微信消息。单条消息失败时只记录错误，继续处理后续消息。
     */
    private void messageLoop() throws Exception {
        while (true) {
            List<WeixinMessage> messages;
            try {
                messages = client.getUpdates();
            } catch (Exception exception) {
                // 网络抖动不应终止 Bot；等待后继续复用当前登录会话。
                System.err.println("微信消息轮询失败，5 秒后重试：" + exception.getMessage());
                Thread.sleep(5000);
                continue;
            }

            for (WeixinMessage message : messages) {
                try {
                    messageHandler.handle(message);
                } catch (Exception e) {
                    System.err.println("处理微信消息失败：" + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 暂时使用轮询间隔，后续可以根据 SDK 是否支持长轮询再优化。
            Thread.sleep(1000);
        }
    }
}
