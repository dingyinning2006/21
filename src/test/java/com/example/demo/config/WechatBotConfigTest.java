package com.example.demo.config;

import com.github.wechat.ilink.sdk.ILinkClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WechatBotConfigTest {

    @Test
    void disablesSdkPollingWhenApplicationOwnsMessageLoop() {
        ILinkClient client = new WechatBotConfig().iLinkClient(
                new WechatSessionStore("target/wechat-config-test-session.json")
        );

        try {
            assertFalse(client.getConfig().isHeartbeatEnabled());
        } finally {
            client.close();
        }
    }
}
