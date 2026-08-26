package com.example.demo.rag;

import java.util.List;

/**
 * 心理健康检索结果（M2-002 统一检索结果结构）
 * 包含是否命中、命中文档、原始查询、澄清建议
 */
public record MentalHealthRetrievalResult(
        /** 原始查询 */
        String query,
        /** 是否命中知识库 */
        boolean matched,
        /** 命中文档列表（按匹配度排序） */
        List<MentalHealthDocument> documents,
        /** 无命中时的澄清建议（命中时为空） */
        String clarification
) {

    /**
     * 创建命中结果
     */
    public static MentalHealthRetrievalResult of(String query, List<MentalHealthDocument> documents) {
        return new MentalHealthRetrievalResult(query, true, documents, "");
    }

    /**
     * 创建未命中结果
     */
    public static MentalHealthRetrievalResult empty(String query, String clarification) {
        return new MentalHealthRetrievalResult(query, false, List.of(), clarification);
    }

    /**
     * 获取命中数量
     */
    public int matchCount() {
        return documents == null ? 0 : documents.size();
    }
}
