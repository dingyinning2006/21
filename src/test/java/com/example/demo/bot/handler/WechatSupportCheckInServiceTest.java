package com.example.demo.bot.handler;

import com.example.demo.storage.InMemorySupportStateStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatSupportCheckInServiceTest {

    @Test
    void savesWechatTextCheckInAndReadsHistory() {
        InMemorySupportStateStore store = new InMemorySupportStateStore();
        WechatSupportCheckInService service = new WechatSupportCheckInService(store);

        String saved = service.handle("wx-user-1", "打卡 压力=6 睡眠=7 心情=5 完成率=0.5 备注=完成最小任务").orElseThrow();

        assertTrue(saved.contains("今日打卡已保存"));
        assertEquals(1, store.findCheckIns("wx-user-1", LocalDate.now(), LocalDate.now()).size());
        assertTrue(service.handle("wx-user-1", "查看打卡").orElseThrow().contains("压力 6/10"));
    }

    @Test
    void returnsMissingFieldPromptWithoutWritingRecord() {
        InMemorySupportStateStore store = new InMemorySupportStateStore();
        WechatSupportCheckInService service = new WechatSupportCheckInService(store);

        String reply = service.handle("wx-user-2", "打卡 压力=8 睡眠=5").orElseThrow();

        assertTrue(reply.contains("缺少：心情、完成率"));
        assertTrue(store.findState("wx-user-2").isEmpty());
    }

    @Test
    void keepsUnrelatedMessagesOnExistingBotRoute() {
        WechatSupportCheckInService service = new WechatSupportCheckInService(new InMemorySupportStateStore());

        assertEquals(java.util.Optional.empty(), service.handle("wx-user-3", "明天天气怎么样"));
    }
}
