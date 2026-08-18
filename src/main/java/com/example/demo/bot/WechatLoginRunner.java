package com.example.demo.bot;

import com.example.demo.config.WechatSessionStore;
import com.example.demo.image.ImageService;
import com.example.demo.intent.IntentResult;
import com.example.demo.intent.IntentService;
import com.example.demo.llm.QwenService;
import com.example.demo.vision.VisionService;
import com.example.demo.voice.VoiceTranscriptionService;
import com.example.demo.weather.WeatherService;
import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WechatLoginRunner implements ApplicationRunner {

    private final ILinkClient client;
    private final QwenService qwenService;
    private final IntentService intentService;
    private final ImageService imageService;
    private final WechatSessionStore sessionStore;
    private final VisionService visionService;
    private final VoiceTranscriptionService voiceTranscriptionService;
    private final WeatherService weatherService;

    public WechatLoginRunner(
            ILinkClient client,
            QwenService qwenService,
            IntentService intentService,
            ImageService imageService,
            WechatSessionStore sessionStore,
            VisionService visionService,
            VoiceTranscriptionService voiceTranscriptionService,
            WeatherService weatherService
    ) {
        this.client = client;
        this.qwenService = qwenService;
        this.intentService = intentService;
        this.imageService = imageService;
        this.sessionStore = sessionStore;
        this.visionService = visionService;
        this.voiceTranscriptionService = voiceTranscriptionService;
        this.weatherService = weatherService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LoginContext context = client.getLoginContext();

        if (context == null) {
            String qrCodeContent = client.executeLogin();
            System.out.println("请将下面内容生成二维码，然后用手机微信扫码登录：");
            System.out.println(qrCodeContent);

            context = client.getLoginFuture().get();
            sessionStore.save(context);
            System.out.println("登录成功，botId = " + context.getBotId());
        } else {
            System.out.println("已恢复微信登录，botId = " + context.getBotId());
        }

        while (true) {
            List<WeixinMessage> messages = client.getUpdates();

            for (WeixinMessage message : messages) {
                String fromUserId = message.getFrom_user_id();
                String userText = getText(message);

                if (fromUserId == null || fromUserId.isBlank()) {
                    continue;
                }

                MessageItem voiceItem = getFirstVoiceItem(message);
                if (voiceItem != null) {
                    client.sendText(fromUserId, "收到语音，正在识别...");

                    byte[] voiceBytes = client.downloadVoiceFromMessageItem(voiceItem);
                    userText = voiceTranscriptionService.transcribeSilk(voiceBytes);
                    System.out.println("语音识别结果：" + userText);

                    if (userText == null || userText.isBlank()) {
                        client.sendText(fromUserId, "这段语音我没有识别出文字，可以再说一遍。");
                        continue;
                    }

                    if (userText.startsWith("语音识别失败") || userText.startsWith("语音识别异常")) {
                        client.sendText(fromUserId, userText);
                        continue;
                    }
                }

                MessageItem imageItem = getFirstImageItem(message);
                if (imageItem != null) {
                    String prompt = userText == null || userText.isBlank()
                            ? "请简洁描述这张图片的主要内容。"
                            : userText;
                    byte[] imageBytes = client.downloadImageFromMessageItem(imageItem);
                    String reply = visionService.understandImage(imageBytes, prompt);
                    client.sendText(fromUserId, reply);
                    System.out.println("已回复图片理解：" + reply);
                    continue;
                }

                if (userText == null || userText.isBlank()) {
                    userText = "用户发送了一条非文字消息";
                }

                System.out.println("收到消息：" + userText);

                IntentResult intent = intentService.recognize(userText);
                System.out.println("识别意图：" + intent.getReplyType() + "，内容：" + intent.getUserQuestion());

                if ("image".equals(intent.getReplyType())) {
                    client.sendText(fromUserId, "正在生成图片，请稍等...");

                    byte[] imageBytes = imageService.generateImage(intent.getUserQuestion());

                    client.sendImage(
                            fromUserId,
                            imageBytes,
                            "wanx-image.png",
                            "已为你生成图片"
                    );

                    System.out.println("已发送图片");
                    continue;
                }

                if ("weather".equals(intent.getReplyType())) {
                    System.out.println("开始查询天气：" + intent.getUserQuestion());
                    String reply = weatherService.query(intent.getUserQuestion());
                    client.sendText(fromUserId, reply);
                    System.out.println("已回复天气：" + reply);
                    continue;
                }

                String reply = qwenService.chat(intent.getUserQuestion());
                client.sendText(fromUserId, reply);
                System.out.println("已回复：" + reply);
            }

            Thread.sleep(1000);
        }
    }

    private String getText(WeixinMessage message) {
        if (message.getItem_list() == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();

        for (MessageItem item : message.getItem_list()) {
            if (item.getText_item() != null) {
                text.append(item.getText_item().getText());
            }
        }

        return text.toString();
    }

    private MessageItem getFirstImageItem(WeixinMessage message) {
        if (message.getItem_list() == null) {
            return null;
        }

        for (MessageItem item : message.getItem_list()) {
            if (item.getImage_item() != null) {
                return item;
            }
        }

        return null;
    }

    private MessageItem getFirstVoiceItem(WeixinMessage message) {
        if (message.getItem_list() == null) {
            return null;
        }

        for (MessageItem item : message.getItem_list()) {
            if (item.getVoice_item() != null) {
                return item;
            }
        }

        return null;
    }
}
