package com.example.demo.bot.handler;

import com.example.demo.agent.planning.PlanAdjustmentService;
import com.example.demo.agent.planning.SevenDayPlanGenerator;
import com.example.demo.agent.planning.SupportPlanOrchestrator;
import com.example.demo.agent.safety.SafetyRouter;
import com.example.demo.agent.screening.ScreeningOrchestrator;
import com.example.demo.audio.speech.VoiceTranscriptionService;
import com.example.demo.audio.tts.TtsService;
import com.example.demo.image.ImageService;
import com.example.demo.intent.IntentService;
import com.example.demo.llm.QwenService;
import com.example.demo.rag.KeywordRagService;
import com.example.demo.skill.SkillKeywordRouter;
import com.example.demo.vision.VisionService;
import com.example.demo.weather.WeatherService;
import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WechatMessageHandlerPlanFlowTest {

    @Test
    void completesScreeningCheckInAdjustmentAndReportFlow() throws Exception {
        ILinkClient client = mock(ILinkClient.class);
        WechatMessageHandler handler = new WechatMessageHandler(
                client,
                mock(QwenService.class),
                mock(IntentService.class),
                mock(ImageService.class),
                mock(VisionService.class),
                mock(VoiceTranscriptionService.class),
                mock(TtsService.class),
                mock(WeatherService.class),
                mock(SkillKeywordRouter.class),
                mock(KeywordRagService.class),
                new ScreeningOrchestrator(),
                new SafetyRouter(),
                new SupportPlanOrchestrator(new SevenDayPlanGenerator(), new PlanAdjustmentService())
        );

        send(handler, "e2e-user", "我下周要面试，最近每天失眠、焦虑");
        send(handler, "e2e-user", "压力 8 分");
        send(handler, "e2e-user", "最近失眠");
        send(handler, "e2e-user", "已经影响求职");
        send(handler, "e2e-user", "今日打卡：压力 9 分，睡眠 5 小时，心情 4 分，任务完成率 0%");
        send(handler, "e2e-user", "阶段报告");

        verify(client, atLeast(1)).sendText(eq("e2e-user"), org.mockito.ArgumentMatchers.contains("已为你生成 7 天计划"));
        verify(client).sendText(eq("e2e-user"), org.mockito.ArgumentMatchers.contains("明天的现实任务调整为更小的一步"));
        verify(client).sendText(eq("e2e-user"), org.mockito.ArgumentMatchers.contains("阶段报告："));
    }

    private void send(WechatMessageHandler handler, String userId, String text) throws Exception {
        WeixinMessage message = new WeixinMessage();
        message.setFrom_user_id(userId);
        message.setItem_list(List.of(MessageItem.text(text)));
        handler.handle(message);
    }
}
