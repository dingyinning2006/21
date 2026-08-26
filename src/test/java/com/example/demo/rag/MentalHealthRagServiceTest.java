package com.example.demo.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M2-002 心理健康 RAG 服务单元测试
 * 验证：命中检索、无命中澄清请求、不编造来源、开关控制
 */
class MentalHealthRagServiceTest {

    private final MentalHealthKnowledgeBase knowledgeBase = new MentalHealthKnowledgeBase();
    private final MentalHealthRagService ragService = new MentalHealthRagService(knowledgeBase);

    @Test
    void shouldReturnMatchedResultForRelevantQuery() {
        MentalHealthRetrievalResult result = ragService.retrieve("失眠怎么办");
        assertTrue(result.matched(), "相关查询应命中");
        assertTrue(result.matchCount() > 0, "应返回至少1篇文档");
        assertTrue(result.clarification().isEmpty(), "命中时澄清建议应为空");
    }

    @Test
    void shouldReturnEmptyWithClarificationForIrrelevantQuery() {
        // 验收标准：无命中时返回空结果或澄清请求，不编造来源
        MentalHealthRetrievalResult result = ragService.retrieve("今天吃什么");
        assertFalse(result.matched(), "不相关查询不应命中");
        assertEquals(0, result.matchCount(), "不应返回文档");
        assertFalse(result.clarification().isEmpty(), "无命中时应返回澄清建议");
    }

    @Test
    void shouldNotFabricateSourcesWhenNoMatch() {
        MentalHealthRetrievalResult result = ragService.retrieve("外星人存在吗");
        assertFalse(result.matched());
        // 澄清建议不应包含编造的知识内容
        String clarification = result.clarification();
        assertFalse(clarification.contains("根据研究"), "无命中时不应编造来源");
        assertFalse(clarification.contains("专家指出"), "无命中时不应编造来源");
    }

    @Test
    void shouldReturnClarificationForPsychRelatedButNoMatch() {
        // 心理相关但知识库没有对应内容时，应给出更具体的澄清
        MentalHealthRetrievalResult result = ragService.retrieve("抑郁症怎么治疗");
        assertFalse(result.matched());
        assertTrue(result.clarification().contains("专业"), "涉及心理问题时应建议专业帮助");
    }

    @Test
    void isHitShouldReturnTrueForRelevantQuery() {
        assertTrue(ragService.isHit("焦虑"));
        assertTrue(ragService.isHit("面试紧张"));
        assertFalse(ragService.isHit("今天星期几"));
    }

    @Test
    void buildContextShouldReturnEmptyWhenNoMatch() {
        String context = ragService.buildContext("今天天气真好");
        assertTrue(context.isEmpty(), "无命中时buildContext应返回空字符串");
    }

    @Test
    void buildContextShouldReturnContentWhenMatch() {
        String context = ragService.buildContext("失眠");
        assertFalse(context.isEmpty());
        assertTrue(context.contains("睡眠"), "命中时应返回相关知识内容");
    }

    @Test
    void shouldHandleBlankQuery() {
        MentalHealthRetrievalResult result = ragService.retrieve("");
        assertFalse(result.matched());
        assertFalse(result.clarification().isEmpty());

        result = ragService.retrieve(null);
        assertFalse(result.matched());
    }

    @Test
    void shouldBeEnabledByDefault() {
        assertTrue(ragService.isEnabled(), "RAG默认应开启");
    }

    @Test
    void shouldReturnEmptyWhenDisabled() {
        MentalHealthRagService disabledService = new MentalHealthRagService(knowledgeBase);
        disabledService.setEnabled(false);
        MentalHealthRetrievalResult result = disabledService.retrieve("失眠");
        assertFalse(result.matched(), "RAG关闭时不应命中");
    }
}
