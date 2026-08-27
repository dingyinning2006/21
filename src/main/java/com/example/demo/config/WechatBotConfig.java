package com.example.demo.config;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.ILinkClientBuilder;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.context.ResumeContext;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
// 微信客户端配置类：把 SDK 客户端交给 Spring 管理，供机器人主循环使用。
public class WechatBotConfig {

    @Bean(destroyMethod = "close")
    public ILinkClient iLinkClient(WechatSessionStore sessionStore) {
        // 这些参数控制网络超时、失败重试和心跳，避免微信连接长时间无响应。
        ILinkConfig config = ILinkConfig.builder()
                .connectTimeoutMs(35000)
                .readTimeoutMs(35000)
                .writeTimeoutMs(35000)
                .httpMaxRetries(3)
                .retryBaseDelayMs(1000)
                .retryMaxDelayMs(10000)
                // 消息由 WechatLoginRunner 的单一手动轮询消费，避免 SDK 心跳线程并行调用 getUpdates。
                .heartbeatEnabled(false)
                .heartbeatIntervalMs(30000)
                .channelVersion("1.0.0")
                .build();

        ILinkClientBuilder builder = ILinkClient.builder()
                .config(config);

        Optional<LoginContext> loginContext = sessionStore.load();

        if (loginContext.isPresent()) {
            // 找到上次保存的登录信息时，尝试恢复会话，减少重复扫码。
            System.out.println("检测到历史微信登录会话，尝试恢复登录");
            builder.resumeContext(ResumeContext.of(loginContext.get()));
        }

        return builder.build();
    }
}
