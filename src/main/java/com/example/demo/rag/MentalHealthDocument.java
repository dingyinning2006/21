package com.example.demo.rag;

import java.util.List;

/**
 * 心理健康知识文档（M2-001）
 * 在 M1 的 RagDocument 基础上扩展元数据：来源、适用范围、更新时间、场景分类、风险等级
 * 不修改 M1 的 RagDocument 契约，独立定义心理健康领域的文档结构
 */
public record MentalHealthDocument(
        /** 文档标题 */
        String title,
        /** 检索关键词 */
        List<String> keywords,
        /** 知识内容 */
        String content,
        /** 场景分类：睡眠/呼吸/压力管理/拖延/人际沟通/求职考试 */
        String category,
        /** 信息来源（权威机构名称，如"世界卫生组织"、"美国心理学会"） */
        String source,
        /** 适用范围描述 */
        String applicableScope,
        /** 更新时间（YYYY-MM-DD） */
        String updateTime,
        /** 风险等级：normal（普通调适建议）/ seek_professional（建议寻求专业帮助） */
        String riskLevel
) {

    /**
     * 转换为 M1 的 RagDocument，供 KeywordRagService 兼容使用
     */
    public RagDocument toRagDocument() {
        String contentWithSource = content + "\n\n来源：" + source + "（更新于 " + updateTime + "）";
        return new RagDocument(title, keywords, contentWithSource);
    }

    /**
     * 判断用户查询是否命中文档关键词
     */
    public boolean matches(String query) {
        if (query == null || query.isBlank() || keywords == null) {
            return false;
        }
        String lower = query.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算匹配得分（命中关键词数量）
     */
    public int calculateScore(String query) {
        if (query == null || query.isBlank() || keywords == null) {
            return 0;
        }
        int score = 0;
        for (String keyword : keywords) {
            if (query.toLowerCase().contains(keyword.toLowerCase())) {
                score++;
            }
        }
        return score;
    }
}
