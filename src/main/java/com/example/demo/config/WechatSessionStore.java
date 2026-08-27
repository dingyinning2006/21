package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
// 负责把微信登录会话持久化到本地文件，以便应用重启后恢复登录状态。
public class WechatSessionStore {

    private final Path sessionFile;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatSessionStore(
            @Value("${wechat.session-file:data/wechat-login.json}") String sessionFile
    ) {
        this.sessionFile = Path.of(sessionFile);
    }

    public Optional<LoginContext> load() {
        try {
            // 文件不存在表示第一次登录，调用方需要走扫码登录流程。
            if (!Files.exists(sessionFile)) {
                return Optional.empty();
            }

            LoginSession session = objectMapper.readValue(sessionFile.toFile(), LoginSession.class);

            if (session.botToken == null || session.userId == null || session.botId == null || session.baseUrl == null) {
                return Optional.empty();
            }

            return Optional.of(new LoginContext(
                    session.botToken,
                    session.userId,
                    session.botId,
                    session.baseUrl
            ));
        } catch (Exception e) {
            System.err.println("读取微信登录会话失败，将重新扫码：" + e.getMessage());
            return Optional.empty();
        }
    }

    public void save(LoginContext context) {
        try {
            // 保存前创建父目录，避免首次运行时因为 data 目录不存在而失败。
            Path parent = sessionFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            LoginSession session = new LoginSession();
            session.botToken = context.getBotToken();
            session.userId = context.getUserId();
            session.botId = context.getBotId();
            session.baseUrl = context.getBaseUrl();

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(sessionFile.toFile(), session);
            System.out.println("已保存微信登录会话：" + sessionFile);
        } catch (Exception e) {
            System.err.println("保存微信登录会话失败：" + e.getMessage());
        }
    }

    public static class LoginSession {
        public String botToken;
        public String userId;
        public String botId;
        public String baseUrl;
    }
}
