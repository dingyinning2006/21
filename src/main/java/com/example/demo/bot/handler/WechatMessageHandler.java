package com.example.demo.bot.handler;

import com.example.demo.audio.speech.VoiceTranscriptionService;
import com.example.demo.audio.tts.TtsService;
import com.example.demo.image.ImageService;
import com.example.demo.intent.IntentResult;
import com.example.demo.intent.IntentService;
import com.example.demo.llm.QwenService;
import com.example.demo.vision.VisionService;
import com.example.demo.weather.WeatherService;
import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.springframework.stereotype.Component;

/**
 * 负责处理一条微信消息。
 *
 * 微信连接和消息业务分开后，主循环只需要负责拉取消息，
 * 这里负责判断消息类型、调用对应服务并发送回复。
 */
@Component
public class WechatMessageHandler {

    private final ILinkClient client;
    private final QwenService qwenService;
    private final IntentService intentService;
    private final ImageService imageService;
    private final VisionService visionService;
    private final VoiceTranscriptionService voiceTranscriptionService;
    private final TtsService ttsService;
    private final WeatherService weatherService;

    public WechatMessageHandler(
            ILinkClient client,
            QwenService qwenService,
            IntentService intentService,
            ImageService imageService,
            VisionService visionService,
            VoiceTranscriptionService voiceTranscriptionService,
            TtsService ttsService,
            WeatherService weatherService
    ) {
        this.client = client;
        this.qwenService = qwenService;
        this.intentService = intentService;
        this.imageService = imageService;
        this.visionService = visionService;
        this.voiceTranscriptionService = voiceTranscriptionService;
        this.ttsService = ttsService;
        this.weatherService = weatherService;
    }

    /**
     * 处理一条消息，并把结果发送给消息发送者。
     * 单条消息的异常由上层主循环统一捕获，避免一个消息影响后续消息。
     */
    public void handle(WeixinMessage message) throws Exception {
        String fromUserId = message.getFrom_user_id();
        if (fromUserId == null || fromUserId.isBlank()) {
            return;
        }

        String userText = getText(message);

        MessageItem voiceItem = getFirstVoiceItem(message);
        if (voiceItem != null) {
            userText = handleVoiceMessage(fromUserId, voiceItem);
            if (userText == null || userText.isBlank()) {
                return;
            }
            if (userText.startsWith("语音识别失败") || userText.startsWith("语音识别异常")) {
                client.sendText(fromUserId, userText);
                return;
            }
        }

        MessageItem imageItem = getFirstImageItem(message);
        if (imageItem != null) {
            handleImageMessage(fromUserId, imageItem, userText);
            return;
        }

        if (userText == null || userText.isBlank()) {
            userText = "用户发送了一条非文字消息";
        }

        System.out.println("收到消息：" + userText);

        IntentResult intent = intentService.recognize(userText);
        System.out.println("识别意图：" + intent.getReplyType()
                + "，内容：" + intent.getUserQuestion());

        handleIntent(fromUserId, intent);
    }

    private String handleVoiceMessage(String fromUserId, MessageItem voiceItem) throws Exception {
        // 先给用户即时反馈，再执行可能耗时的下载、解码和语音识别。
        client.sendText(fromUserId, "收到语音，正在识别...");

        byte[] voiceBytes = client.downloadVoiceFromMessageItem(voiceItem);
        String userText = voiceTranscriptionService.transcribeSilk(voiceBytes);
        System.out.println("语音识别结果：" + userText);

        if (userText == null || userText.isBlank()) {
            client.sendText(fromUserId, "这段语音我没有识别出文字，可以再说一遍。");
        }

        return userText;
    }

    private void handleImageMessage(
            String fromUserId,
            MessageItem imageItem,
            String userText
    ) throws Exception {
        // 图片消息的文字部分作为提示词，没有文字时使用默认描述请求。
        String prompt = userText == null || userText.isBlank()
                ? "请简洁描述这张图片的主要内容。"
                : userText;

        byte[] imageBytes = client.downloadImageFromMessageItem(imageItem);
        String reply = visionService.understandImage(imageBytes, prompt);
        client.sendText(fromUserId, reply);
        System.out.println("已回复图片理解：" + reply);
    }

    private void handleIntent(String fromUserId, IntentResult intent) throws Exception {
        // 意图识别后按回复类型分流，避免所有请求都进入通用聊天模型。
        if ("voice".equals(intent.getReplyType())) {
            handleVoiceIntent(fromUserId, intent.getUserQuestion());
            return;
        }

        if ("image".equals(intent.getReplyType())) {
            handleImageIntent(fromUserId, intent.getUserQuestion());
            return;
        }

        if ("weather".equals(intent.getReplyType())) {
            String reply = weatherService.query(intent.getUserQuestion());
            client.sendText(fromUserId, reply);
            System.out.println("已回复天气：" + reply);
            return;
        }

        // text 类型由 QwenService 处理，工具调用也在其中完成。
        String reply = qwenService.chat(intent.getUserQuestion());
        client.sendText(fromUserId, reply);
        System.out.println("已回复：" + reply);
    }

    /**
     * 语音回复不是直接把用户问题转成语音，而是先得到正常的文字答案。
     * 这样“查询天气并用语音回答”也会先走天气服务，再朗读天气结果。
     */
    private void handleVoiceIntent(String fromUserId, String userQuestion) throws Exception {
        IntentResult contentIntent = intentService.recognize(userQuestion);
        String textReply = buildTextReply(contentIntent);

        byte[] wavBytes = ttsService.synthesize(textReply);

        // 按文件发送 WAV，不走微信语音消息接口，也不需要转换成 SILK。
        client.sendFile(
                fromUserId,
                wavBytes,
                "qwen-reply.wav",
                ""
        );

        System.out.println("已发送 WAV 文件回复：" + textReply);
    }

    private String buildTextReply(IntentResult intent) {
        if ("weather".equals(intent.getReplyType())) {
            return weatherService.query(intent.getUserQuestion());
        }

        if ("text".equals(intent.getReplyType())) {
            return qwenService.chat(intent.getUserQuestion());
        }

        return "目前只能把文字答案转换成语音，暂不支持朗读图片或直接朗读图片。";
    }

    private void handleImageIntent(String fromUserId, String prompt) throws Exception {
        // 文生图通常耗时较长，因此先发送处理中提示，再发送图片结果。
        client.sendText(fromUserId, "正在生成图片，请稍等...");

        byte[] imageBytes = imageService.generateImage(prompt);
        client.sendImage(
                fromUserId,
                imageBytes,
                "wanx-image.png",
                "已为你生成图片"
        );

        System.out.println("已发送图片");
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
