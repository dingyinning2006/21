package com.example.demo.bot;

import com.github.wechat.ilink.sdk.core.listener.OnLoginListener;
import com.github.wechat.ilink.sdk.core.listener.OnMessageListener;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.TextItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.github.wechat.ilink.sdk.ILinkClient;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class BotRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BotRunner.class);

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private ILinkClient client;

    private static final String FIXED_REPLY = "我是你的机器人，请等待问答";

    @Override
    public void run(String... args) throws Exception {
        client = ILinkClient.builder()
                .onLogin(new OnLoginListener() {
                    @Override
                    public void onLoginSuccess(LoginContext context) {
                        logger.info("登录成功，botId={}",context.getBotId());
                    }

                    @Override
                    public void onLoginFailure(Throwable throwable) {
                        logger.error("登录失败：{}",throwable.getMessage());
                    }
                }).onMessage(new OnMessageListener() {
                    @Override
                    public void onMessages(List<WeixinMessage> messages) {
                        handleMessages(messages);
                    }
                })
                .build();

        LoginContext context = loginWithQrRetry();
        if (context==null){
            logger.error("多次尝试后仍登录失败，请重启程序再尝试");
            return;
        }
        logger.info("登录完成，botId = {}",context.getBotId());

        // 长轮询收消息：getUpdates() 阻塞等新消息，收到后自动触发 onMessage。
        // SDK 没有后台线程，这个循环必须自己写。
        while (true) {
            try {
                List<WeixinMessage> messages = client.getUpdates();
                if (messages.isEmpty()) {
                    Thread.sleep(500); // 空轮询稍等片刻，避免空转
                }
            } catch (Exception e) {
                logger.warn("拉取消息失败: {}", e.getMessage());
                Thread.sleep(2000); // 出错后等 2 秒再继续，避免疯狂重试
            }
        }

    }

    private LoginContext loginWithQrRetry() throws Exception{
        for (int attempt = 1;attempt<=MAX_LOGIN_ATTEMPTS;attempt++){
            String qrContent = client.executeLogin();
            generateQrCode(qrContent, Paths.get("qr.png"));
            logger.info("第{}次生成二维码，请打开项目根目录下的qr.png，用手机微信扫码登录",attempt);
            try {
                return client.getLoginFuture().get(2, TimeUnit.MINUTES);
            }catch (TimeoutException e){
                logger.warn("二维码已过期，重新生成...");
                client.cancelLogin();
            }catch (ExecutionException e){
                logger.warn("登录失败：{}，重新生成二维码...",e.getMessage());
                client.cancelLogin();
            }

        }
        return null;
    }

    private void generateQrCode(String content, Path filePath) throws Exception{
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET,"UTF-8");
        hints.put(EncodeHintType.MARGIN,1);
        BitMatrix matrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE,400,400,hints);
        MatrixToImageWriter.writeToPath(matrix,"PNG",filePath);
    }

    private void handleMessages(List<WeixinMessage> messages){
        for (WeixinMessage msg:messages){
            String fromUserId = msg.getFrom_user_id();
            List<MessageItem> items = msg.getItem_list();
            if (items==null){
                continue;
            }

            for (MessageItem item:items){
                TextItem textItem = item.getText_item();
                if (textItem==null){
                    continue;
                }
                logger.info("收到消息 fromUserId={},text={}",fromUserId,textItem.getText());
                try {
                    client.sendText(fromUserId,FIXED_REPLY);
                }catch (Exception e){
                    logger.error("回复失败：{}",e.getMessage());
                }
            }
        }
    }
}
