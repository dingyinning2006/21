package com.example.demo.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 心理健康 RAG 服务（M2-002）
 * 基于关键词检索的心理健康知识检索服务
 * 统一检索结果结构，无命中时返回澄清请求，不编造来源
 */
@Service
public class MentalHealthRagService {

    private final MentalHealthKnowledgeBase knowledgeBase;

    @Value("${rag.mental-health.enabled:true}")
    private boolean enabled = true;

    public MentalHealthRagService(MentalHealthKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 检索心理健康知识，返回统一结果
     *
     * @param query 用户查询
     * @return 检索结果（包含是否命中、命中文档、澄清建议）
     */
    public MentalHealthRetrievalResult retrieve(String query) {
        if (!enabled || query == null || query.isBlank()) {
            return MentalHealthRetrievalResult.empty(query, "查询为空，请描述你想了解的心理调适问题。");
        }

        List<MentalHealthDocument> documents = knowledgeBase.retrieve(query);

        if (documents.isEmpty()) {
            String clarification = buildClarification(query);
            return MentalHealthRetrievalResult.empty(query, clarification);
        }

        return MentalHealthRetrievalResult.of(query, documents);
    }

    /**
     * 构建 RAG 增强的上下文文本（带来源引用）
     */
    public String buildContext(String query) {
        MentalHealthRetrievalResult result = retrieve(query);
        if (!result.matched()) {
            return "";
        }
        return knowledgeBase.buildContext(query);
    }

    /**
     * 判断是否命中心理健康知识
     */
    public boolean isHit(String query) {
        return retrieve(query).matched();
    }

    /**
     * 无命中时生成澄清建议，不编造来源
     */
    private String buildClarification(String query) {
        // 检查是否包含心理相关词汇但知识库没有对应内容
        boolean isPsychRelated = query.contains("心理") || query.contains("情绪") || query.contains("压力")
                || query.contains("焦虑") || query.contains("抑郁") || query.contains("睡眠")
                || query.contains("失眠") || query.contains("人际") || query.contains("沟通");

        if (isPsychRelated) {
            return "我暂时没有找到与该问题直接相关的调适知识。你可以尝试更具体地描述，"
                    + "例如：\"失眠怎么办\"、\"面试紧张如何缓解\"、\"拖延症怎么克服\"。"
                    + "如果问题持续影响你的生活，建议咨询专业心理咨询师。";
        }

        return "我主要提供压力调适、睡眠改善、呼吸训练、人际沟通、求职考试压力等方面的心理健康知识。"
                + "你可以问我：\"失眠怎么办\"、\"焦虑如何缓解\"、\"面试紧张\"、\"拖延症\"等。";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
