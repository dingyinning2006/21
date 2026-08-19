package com.example.demo.bot;

import com.github.wechat.ilink.sdk.core.listener.OnLoginListener;
import com.github.wechat.ilink.sdk.core.listener.OnMessageListener;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.TextItem;
import com.github.wechat.ilink.sdk.core.model.VoiceItem;
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


import tools.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private final LLMService llmService;
    private final WeatherService weatherService;
    private final TTSService ttsService;

    public BotRunner(LLMService llmService,
                     WeatherService weatherService, TTSService ttsService) {
        this.llmService = llmService;
        this.weatherService = weatherService;
        this.ttsService = ttsService;
    }


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
                if (item.getImage_item()!=null){
                    handleImageMessage(fromUserId,item);
                    continue;
                }
                if (item.getVoice_item()!=null){
                    handleVoiceMessage(fromUserId,item);
                    continue;
                }
                TextItem textItem = item.getText_item();
                if (textItem==null){
                    continue;
                }
                handleUserMessage(fromUserId, textItem.getText());
            }
        }
    }

    private void handleImageMessage(String fromUserId, MessageItem item){
        try {
            byte[] imageBytes = client.downloadImageFromMessageItem(item);
            logger.info("收到图片消息 fromUserId={}, size={} bytes", fromUserId, imageBytes.length);
            String reply = llmService.describeImage(imageBytes,null);
            client.sendText(fromUserId, reply);
        }catch (Exception e){
            logger.error("图片识别失败：{}",e.getMessage());
            try {
                client.sendText(fromUserId,"抱歉，我暂时无法识别这张图片");
            }catch (Exception e2){
                logger.error("发送兜底回复也失败：{}",e2.getMessage());
            }
        }
    }

    /** 文字消息：走带 Function Calling 的对话（模型自主决定调天气/时间工具）；返回回复文本，供语音场景复用 */
    private String handleUserMessage(String fromUserId, String userText){
        logger.info("收到消息 fromUserId={},text={}", fromUserId, userText);
        try {
            String reply = llmService.chatWithTools(fromUserId, userText, this::executeTool);
            client.sendText(fromUserId, reply);
            return reply;
        }catch (Exception e){
            logger.error("回复失败：{}",e.getMessage());
            try {
                client.sendText(fromUserId,"抱歉，我暂时无法回答，请稍后再试");
            }catch (Exception e2){
                logger.error("发送兜底回复也失败：{}",e2.getMessage());
            }
            return null;
        }
    }

    /** 工具执行器：模型请求工具时由这里分发执行，返回文本结果给模型 */
    private String executeTool(String toolName, JsonNode args) throws Exception {
        switch (toolName) {
            case "get_weather":
                String city = args.path("city").isValueNode() ? args.path("city").asText() : null;
                return weatherService.getWeather(city);
            case "get_current_time":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss"));
            case "calculate":
                String expr = args.path("expression").isValueNode() ? args.path("expression").asText() : null;
                if (expr == null || expr.isBlank()) {
                    throw new IllegalArgumentException("缺少表达式参数");
                }
                double result = Calculator.evaluate(expr);
                // 保留 8 位小数去尾零，消除浮点噪声（如 0.1+0.2 -> 0.3 而不是 0.30000000000000004）
                return new java.math.BigDecimal(String.format("%.8f", result))
                        .stripTrailingZeros().toPlainString();
            default:
                throw new IllegalStateException("未知工具：" + toolName);
        }
    }

    /** 语音消息：微信自带语音转文字，转写成功后文字回复 + 追加一条语音文件回复 */
    private void handleVoiceMessage(String fromUserId, MessageItem item){
        VoiceItem voiceItem = item.getVoice_item();
        String voiceText = voiceItem.getText();
        if (voiceText==null || voiceText.isBlank()){
            try {
                client.sendText(fromUserId,"抱歉，我没能听清这条语音");
            }catch (Exception e){
                logger.error("发送兜底回复失败：{}",e.getMessage());
            }
            return;
        }
        logger.info("收到语音消息 fromUserId={}, 转写文字={}", fromUserId, voiceText);
        String reply = handleUserMessage(fromUserId, voiceText);
        if (reply != null){
            sendVoiceReply(fromUserId, reply);
        }
    }

    /** 把文字回复用 TTS 合成语音，转成 silk 后以语音气泡（sendVoice）发给用户；失败不影响已发出的文字回复 */
    private void sendVoiceReply(String fromUserId, String text){
        try {
            TTSService.SilkResult result = ttsService.synthesizeSilk(text);
            // sendVoice(用户, silk字节, 文件名, 播放时长ms, 采样率) —— silk 是微信语音气泡的标准格式
            client.sendVoice(fromUserId, result.silk(), "语音回复.silk", result.playtimeMillis(), 24000);
            logger.info("语音气泡已发送 fromUserId={}, playtime={}ms", fromUserId, result.playtimeMillis());
        }catch (Exception e){
            logger.warn("语音回复发送失败（文字回复已发出）：{}", e.getMessage());
        }
    }

}
