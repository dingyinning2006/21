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
public class WechatBotConfig {

    @Bean(destroyMethod = "close")
    public ILinkClient iLinkClient(WechatSessionStore sessionStore) {
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

        ILinkClientBuilder builder = ILinkClient.builder()
                .config(config);

        Optional<LoginContext> loginContext = sessionStore.load();

        if (loginContext.isPresent()) {
            System.out.println("检测到历史微信登录会话，尝试恢复登录");
            builder.resumeContext(ResumeContext.of(loginContext.get()));
        }

        return builder.build();
    }
}
