package com.example.demo.bot.handler;

import com.example.demo.agent.safety.SafetyRouter;
import com.example.demo.audio.speech.VoiceTranscriptionService;
import com.example.demo.audio.tts.TtsService;
import com.example.demo.image.ImageService;
import com.example.demo.intent.IntentResult;
import com.example.demo.intent.IntentService;
import com.example.demo.llm.QwenService;
import com.example.demo.rag.KeywordRagService;
import com.example.demo.skill.SkillKeywordRouter;
import com.example.demo.agent.screening.ScreeningOrchestrator;
import com.example.demo.agent.planning.SupportPlanOrchestrator;
import com.example.demo.vision.VisionService;
import com.example.demo.weather.WeatherService;
import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WechatMessageHandlerSafetyTest {

    @Test
    void urgentMessageStopsNormalFlow() throws Exception {
        ILinkClient client = mock(ILinkClient.class);
        QwenService qwenService = mock(QwenService.class);
        IntentService intentService = mock(IntentService.class);
        ImageService imageService = mock(ImageService.class);
        VisionService visionService = mock(VisionService.class);
        VoiceTranscriptionService voiceTranscriptionService = mock(VoiceTranscriptionService.class);
        TtsService ttsService = mock(TtsService.class);
        WeatherService weatherService = mock(WeatherService.class);
        SkillKeywordRouter skillKeywordRouter = mock(SkillKeywordRouter.class);
        KeywordRagService keywordRagService = mock(KeywordRagService.class);
        ScreeningOrchestrator screeningOrchestrator = mock(ScreeningOrchestrator.class);
        SupportPlanOrchestrator supportPlanOrchestrator = mock(SupportPlanOrchestrator.class);

        WechatMessageHandler handler = new WechatMessageHandler(
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
                screeningOrchestrator,
                new SafetyRouter(),
                supportPlanOrchestrator
        );

        WeixinMessage message = new WeixinMessage();
        message.setFrom_user_id("user-urgent");
        message.setItem_list(List.of(MessageItem.text("我已经有自杀计划，不想活了")));

        handler.handle(message);

        verify(client).sendText(
                eq("user-urgent"),
                contains("请立即联系身边可信任的人")
        );
        verify(client, never()).sendFile(
                eq("user-urgent"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verifyNoInteractions(
                qwenService,
                intentService,
                skillKeywordRouter,
                keywordRagService,
                screeningOrchestrator
        );
    }

    @Test
    void normalMessageContinuesToExistingFlow() throws Exception {
        ILinkClient client = mock(ILinkClient.class);
        QwenService qwenService = mock(QwenService.class);
        IntentService intentService = mock(IntentService.class);
        ImageService imageService = mock(ImageService.class);
        VisionService visionService = mock(VisionService.class);
        VoiceTranscriptionService voiceTranscriptionService = mock(VoiceTranscriptionService.class);
        TtsService ttsService = mock(TtsService.class);
        WeatherService weatherService = mock(WeatherService.class);
        SkillKeywordRouter skillKeywordRouter = mock(SkillKeywordRouter.class);
        KeywordRagService keywordRagService = mock(KeywordRagService.class);
        ScreeningOrchestrator screeningOrchestrator = mock(ScreeningOrchestrator.class);
        SupportPlanOrchestrator supportPlanOrchestrator = mock(SupportPlanOrchestrator.class);

        when(skillKeywordRouter.route("介绍一下 Spring Boot")).thenReturn(null);
        when(keywordRagService.buildContext("介绍一下 Spring Boot")).thenReturn("");

        IntentResult intent = new IntentResult();
        intent.setReplyType("text");
        intent.setUserQuestion("介绍一下 Spring Boot");
        when(intentService.recognize("介绍一下 Spring Boot")).thenReturn(intent);
        when(qwenService.chat("介绍一下 Spring Boot")).thenReturn("普通流程回复");

        WechatMessageHandler handler = new WechatMessageHandler(
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
                screeningOrchestrator,
                new SafetyRouter(),
                supportPlanOrchestrator
        );

        WeixinMessage message = new WeixinMessage();
        message.setFrom_user_id("user-normal");
        message.setItem_list(List.of(MessageItem.text("介绍一下 Spring Boot")));

        handler.handle(message);

        verify(intentService).recognize("介绍一下 Spring Boot");
        verify(qwenService).chat("介绍一下 Spring Boot");
        verify(client).sendText("user-normal", "普通流程回复");
    }

    @Test
    void supportMessageEntersScreeningBeforeNormalFlow() throws Exception {
        ILinkClient client = mock(ILinkClient.class);
        QwenService qwenService = mock(QwenService.class);
        IntentService intentService = mock(IntentService.class);
        ImageService imageService = mock(ImageService.class);
        VisionService visionService = mock(VisionService.class);
        VoiceTranscriptionService voiceTranscriptionService = mock(VoiceTranscriptionService.class);
        TtsService ttsService = mock(TtsService.class);
        WeatherService weatherService = mock(WeatherService.class);
        SkillKeywordRouter skillKeywordRouter = mock(SkillKeywordRouter.class);
        KeywordRagService keywordRagService = mock(KeywordRagService.class);
        ScreeningOrchestrator screeningOrchestrator = mock(ScreeningOrchestrator.class);
        SupportPlanOrchestrator supportPlanOrchestrator = mock(SupportPlanOrchestrator.class);

        String userText = "我最近因为毕业和求职很焦虑";
        when(screeningOrchestrator.handleFirstMessage("user-support", "user-support", userText))
                .thenReturn("请告诉我现在的压力大约是 0-10 分中的几分？");

        WechatMessageHandler handler = new WechatMessageHandler(
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
                screeningOrchestrator,
                new SafetyRouter(),
                supportPlanOrchestrator
        );

        WeixinMessage message = new WeixinMessage();
        message.setFrom_user_id("user-support");
        message.setItem_list(List.of(MessageItem.text(userText)));

        handler.handle(message);

        verify(screeningOrchestrator).handleFirstMessage("user-support", "user-support", userText);
        verify(client).sendText(
                "user-support",
                "请告诉我现在的压力大约是 0-10 分中的几分？"
        );
        verifyNoInteractions(
                qwenService,
                intentService,
                skillKeywordRouter,
                keywordRagService
        );
    }
}
