package com.example.demo.bot.handler;

import com.example.demo.audio.speech.VoiceTranscriptionService;
import com.example.demo.audio.tts.TtsService;
import com.example.demo.image.ImageService;
import com.example.demo.intent.IntentResult;
import com.example.demo.intent.IntentService;
import com.example.demo.llm.QwenService;
import com.example.demo.skill.SkillKeywordRouter;
import com.example.demo.vision.VisionService;
import com.example.demo.weather.WeatherService;
import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.rag.KeywordRagService;
import com.example.demo.agent.screening.ScreeningOrchestrator;
import com.example.demo.agent.safety.SafetyRouter;
import com.example.demo.agent.contract.SafetyDecision;
import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.StageReport;
import com.example.demo.agent.planning.LocalCheckInParser;
import com.example.demo.agent.planning.SupportPlanOrchestrator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    private final SkillKeywordRouter skillKeywordRouter;
    private final KeywordRagService keywordRagService;
    private final WechatSupportCheckInService supportCheckInService;
    private final ScreeningOrchestrator screeningOrchestrator;
    private final SafetyRouter safetyRouter;
    private final SupportPlanOrchestrator supportPlanOrchestrator;
    private final LocalCheckInParser checkInParser = new LocalCheckInParser();


    @Autowired
    public WechatMessageHandler(
            ILinkClient client,
            QwenService qwenService,
            IntentService intentService,
            ImageService imageService,
            VisionService visionService,
            VoiceTranscriptionService voiceTranscriptionService,
            TtsService ttsService,
            WeatherService weatherService,
            SkillKeywordRouter skillKeywordRouter,
            KeywordRagService keywordRagService,
            WechatSupportCheckInService supportCheckInService,
            ScreeningOrchestrator screeningOrchestrator,
            SafetyRouter safetyRouter,
            SupportPlanOrchestrator supportPlanOrchestrator

    ) {
        this.client = client;
        this.qwenService = qwenService;
        this.intentService = intentService;
        this.imageService = imageService;
        this.visionService = visionService;
        this.voiceTranscriptionService = voiceTranscriptionService;
        this.ttsService = ttsService;
        this.weatherService = weatherService;
        this.skillKeywordRouter = skillKeywordRouter;
        this.keywordRagService = keywordRagService;
        this.supportCheckInService = supportCheckInService;
        this.screeningOrchestrator = screeningOrchestrator;
        this.safetyRouter = safetyRouter;
        this.supportPlanOrchestrator = supportPlanOrchestrator;


    }

    /** 保留不带 M4 存储服务的构造函数，便于 M1 单元测试独立运行。 */
    public WechatMessageHandler(
            ILinkClient client,
            QwenService qwenService,
            IntentService intentService,
            ImageService imageService,
            VisionService visionService,
            VoiceTranscriptionService voiceTranscriptionService,
            TtsService ttsService,
            WeatherService weatherService,
            SkillKeywordRouter skillKeywordRouter,
            KeywordRagService keywordRagService,
            ScreeningOrchestrator screeningOrchestrator,
            SafetyRouter safetyRouter,
            SupportPlanOrchestrator supportPlanOrchestrator
    ) {
        this(
                client,
                qwenService,
                intentService,
                imageService,
                visionService,
                voiceTranscriptionService,
                ttsService,
                weatherService,
                skillKeywordRouter,
                keywordRagService,
                null,
                screeningOrchestrator,
                safetyRouter,
                supportPlanOrchestrator
        );
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

        System.out.println("收到消息，用户ID：" + fromUserId);

        SafetyDecision safetyDecision = safetyRouter.evaluate(fromUserId, userText);
        if (safetyDecision.stopNormalChat()) {
            String safetyMessage = safetyDecision.message()
                    + "\n\n接下来请这样做：\n- "
                    + String.join("\n- ", safetyDecision.actions());
            client.sendText(fromUserId, safetyMessage);
            return;
        }

        if (supportCheckInService != null) {
            String supportReply = supportCheckInService.handle(fromUserId, userText).orElse(null);
            if (supportReply != null) {
                client.sendText(fromUserId, supportReply);
                return;
            }
        }

        if (supportPlanOrchestrator.hasPlan(fromUserId) && isReportRequest(userText)) {
            try {
                StageReport report = supportPlanOrchestrator.buildStageReport(fromUserId);
                client.sendText(fromUserId, formatStageReport(report));
            } catch (IllegalStateException exception) {
                client.sendText(fromUserId, "目前还没有足够的每日打卡记录，完成至少一次打卡后我再为你生成阶段报告。");
            }
            return;
        }

        if (supportPlanOrchestrator.hasPlan(fromUserId)
                && checkInParser.looksLikeCheckIn(userText)) {
            Optional<com.example.demo.agent.contract.CheckInRecord> checkIn = checkInParser.parse(
                    fromUserId,
                    LocalDate.now(),
                    userText
            );
            if (checkIn.isEmpty()) {
                client.sendText(fromUserId,
                        "我可以帮你记录每日打卡，请补充：压力 0-10 分、睡眠几小时、心情 0-10 分、任务完成率百分比。"
                );
                return;
            }

            supportPlanOrchestrator.recordCheckIn(checkIn.get());
            PlanDay nextDay = supportPlanOrchestrator.getPlan(fromUserId).stream()
                    .filter(planDay -> planDay.date().equals(LocalDate.now().plusDays(1)))
                    .findFirst()
                    .orElse(null);
            String adjustmentText = nextDay != null && nextDay.reducedLoad()
                    ? "我已根据今天的状态把明天的现实任务调整为更小的一步。"
                    : "明天先按原计划推进，如果状态变化明显再告诉我。";
            client.sendText(fromUserId, "今天的打卡已记录。" + adjustmentText);
            return;
        }

        if (isSupportMessage(userText)) {
            String screeningReply = screeningOrchestrator.handleFirstMessage(
                    fromUserId,
                    fromUserId,
                    userText
            );
            ScreeningResult screeningResult = screeningOrchestrator.getCompletedResult(fromUserId);
            if (screeningResult == null) {
                client.sendText(fromUserId, screeningReply);
                return;
            }

            List<PlanDay> plan = supportPlanOrchestrator.startPlan(
                    screeningResult,
                    LocalDate.now()
            );
            client.sendText(
                    fromUserId,
                    screeningReply + "\n\n已为你生成 7 天计划。\n" + formatPlanDay(plan.get(0))
            );
            return;
        }

        String skillReply = skillKeywordRouter.route(userText);

        if (skillReply != null) {
            client.sendText(fromUserId, skillReply);
            return;
        }
        String ragContext = keywordRagService.buildContext(userText);

        if (!ragContext.isBlank()) {
            System.out.println("命中 RAG，检索结果：");
            System.out.println(ragContext);

            String ragPrompt = """
            你是一个简洁、可靠的助手。
            请优先根据下面的参考资料回答用户问题。
            如果参考资料无法回答问题，可以基于常识补充，但不要编造资料中没有的具体事实。

            参考资料：
            %s
            """.formatted(ragContext);

            String reply = qwenService.chatWithSystemPrompt(
                    ragPrompt,
                    userText
            );

            client.sendText(fromUserId, reply);
            System.out.println("已回复 RAG 增强结果：" + reply);
            return;
        }

        IntentResult intent = intentService.recognize(userText);
        System.out.println("识别意图，类型：" + intent.getReplyType());

        handleIntent(fromUserId, intent);
    }

    private String handleVoiceMessage(String fromUserId, MessageItem voiceItem) throws Exception {
        // 先给用户即时反馈，再执行可能耗时的下载、解码和语音识别。
        client.sendText(fromUserId, "收到语音，正在识别...");

        byte[] voiceBytes = client.downloadVoiceFromMessageItem(voiceItem);
        String userText = voiceTranscriptionService.transcribeSilk(voiceBytes);
        System.out.println("语音识别完成，用户ID：" + fromUserId);

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
       /* String reply = screeningOrchestrator.handleFirstMessage(
                fromUserId,
                fromUserId,
                intent.getUserQuestion()
        );*/
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

    private boolean isSupportMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String[] keywords = {
                "焦虑", "压力", "毕业", "论文", "答辩", "求职", "面试",
                "考试", "复习", "拖延", "失眠", "睡不着", "睡不好",
                "室友", "家人", "同学", "朋友"
        };
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReportRequest(String message) {
        return message.contains("阶段报告")
                || message.contains("查看报告")
                || message.contains("我的报告");
    }

    private String formatPlanDay(PlanDay planDay) {
        StringBuilder text = new StringBuilder()
                .append("第 ").append(planDay.dayIndex()).append(" 天（")
                .append(planDay.date()).append("）：")
                .append(planDay.focus());
        for (int index = 0; index < planDay.tasks().size(); index++) {
            com.example.demo.agent.contract.PlanTask task = planDay.tasks().get(index);
            text.append("\n").append(index + 1).append(". ")
                    .append(task.title())
                    .append("（约 ").append(task.estimatedDuration().toMinutes()).append(" 分钟）");
        }
        return text.toString();
    }

    private String formatStageReport(StageReport report) {
        return "阶段报告：\n"
                + report.summary() + "\n"
                + "压力变化：" + report.stressDelta() + "\n"
                + "睡眠变化：" + report.sleepDelta() + " 小时\n"
                + "观察：" + String.join("；", report.observedChanges()) + "\n"
                + "下一步：" + String.join("；", report.nextStepSuggestions());
    }
}
