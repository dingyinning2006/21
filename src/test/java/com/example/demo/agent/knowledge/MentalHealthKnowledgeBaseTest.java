package com.example.demo.agent.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M2-001 心理健康知识库单元测试
 * 验证：知识文档数量、场景分布、关键词检索、按场景检索
 */
class MentalHealthKnowledgeBaseTest {

    private final MentalHealthKnowledgeBase knowledgeBase = new MentalHealthKnowledgeBase();

    @Test
    void shouldHave12Documents() {
        List<MentalHealthDocument> all = knowledgeBase.getAllDocuments();
        assertEquals(12, all.size(), "知识库应有12篇文档");
    }

    @Test
    void shouldHave2DocumentsPerCategory() {
        assertEquals(2, knowledgeBase.retrieveByCategory("睡眠").size(), "睡眠类应有2篇");
        assertEquals(2, knowledgeBase.retrieveByCategory("呼吸").size(), "呼吸类应有2篇");
        assertEquals(2, knowledgeBase.retrieveByCategory("压力管理").size(), "压力管理类应有2篇");
        assertEquals(2, knowledgeBase.retrieveByCategory("拖延").size(), "拖延类应有2篇");
        assertEquals(2, knowledgeBase.retrieveByCategory("人际沟通").size(), "人际沟通类应有2篇");
        assertEquals(2, knowledgeBase.retrieveByCategory("求职考试").size(), "求职考试类应有2篇");
    }

    @Test
    void shouldRetrieveSleepDocuments() {
        List<MentalHealthDocument> results = knowledgeBase.retrieve("失眠怎么办");
        assertFalse(results.isEmpty(), "查询'失眠'应命中睡眠类文档");
        assertTrue(results.stream().anyMatch(d -> "睡眠".equals(d.category())),
                "结果中应包含睡眠类文档");
    }

    @Test
    void shouldRetrieveBreathingDocuments() {
        List<MentalHealthDocument> results = knowledgeBase.retrieve("焦虑深呼吸");
        assertFalse(results.isEmpty(), "查询'焦虑深呼吸'应命中呼吸类文档");
    }

    @Test
    void shouldRetrieveInterviewAndSleepForInterviewInsomnia() {
        // 验收标准：用户描述"面试失眠"时能召回睡眠和求职压力相关内容
        List<MentalHealthDocument> results = knowledgeBase.retrieve("面试失眠");
        assertFalse(results.isEmpty(), "查询'面试失眠'应命中文档");
        boolean hasSleep = results.stream().anyMatch(d -> "睡眠".equals(d.category()));
        boolean hasInterview = results.stream().anyMatch(d -> "求职考试".equals(d.category()));
        assertTrue(hasSleep, "应召回睡眠相关内容");
        assertTrue(hasInterview, "应召回求职考试相关内容");
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        List<MentalHealthDocument> results = knowledgeBase.retrieve("今天天气怎么样");
        assertTrue(results.isEmpty(), "不相关查询应返回空结果");
    }

    @Test
    void shouldReturnEmptyForBlankQuery() {
        assertTrue(knowledgeBase.retrieve("").isEmpty());
        assertTrue(knowledgeBase.retrieve(null).isEmpty());
        assertTrue(knowledgeBase.retrieve("   ").isEmpty());
    }

    @Test
    void everyDocumentShouldHaveSourceAndMetadata() {
        // 验收标准：每条包含来源、适用范围和更新时间
        for (MentalHealthDocument doc : knowledgeBase.getAllDocuments()) {
            assertNotNull(doc.source(), "文档'" + doc.title() + "'应有来源");
            assertFalse(doc.source().isBlank(), "文档'" + doc.title() + "'来源不应为空");
            assertNotNull(doc.applicableScope(), "文档'" + doc.title() + "'应有适用范围");
            assertNotNull(doc.updateTime(), "文档'" + doc.title() + "'应有更新时间");
            assertNotNull(doc.category(), "文档'" + doc.title() + "'应有场景分类");
            assertNotNull(doc.riskLevel(), "文档'" + doc.title() + "'应有风险等级");
        }
    }

    @Test
    void shouldNotContainDiagnosisOrDrugAdvice() {
        // 验收标准：建议内容使用非诊断、非药物表述
        for (MentalHealthDocument doc : knowledgeBase.getAllDocuments()) {
            String content = doc.content();
            assertFalse(content.contains("诊断为"), "文档'" + doc.title() + "'不应包含诊断表述");
            assertFalse(content.contains("服用"), "文档'" + doc.title() + "'不应包含药物建议");
            assertFalse(content.contains("处方药"), "文档'" + doc.title() + "'不应包含处方药建议");
        }
    }

    @Test
    void shouldSuggestProfessionalHelpForLongTermIssues() {
        // 验收标准：对长期或明显影响功能的情况给出专业求助建议
        boolean hasProfessionalSuggestion = knowledgeBase.getAllDocuments().stream()
                .anyMatch(d -> d.content().contains("咨询专业")
                        || d.content().contains("寻求专业")
                        || d.content().contains("专业医生")
                        || d.content().contains("心理咨询师"));
        assertTrue(hasProfessionalSuggestion, "知识库中应包含对长期问题建议专业求助的内容");
    }

    @Test
    void buildContextShouldContainSourceCitation() {
        String context = knowledgeBase.buildContext("失眠");
        assertFalse(context.isEmpty());
        assertTrue(context.contains("来源："), "构建的上下文应包含来源引用");
    }
}
