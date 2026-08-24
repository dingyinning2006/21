package com.example.demo.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 关键词检索器：遍历知识库，按关键词命中次数排序，返回最相关的文档片段
 */
@Component
public class KeywordRetriever {

    @Autowired
    private KnowledgeBase knowledgeBase;

    // 返回最相关的前 N 个文档
    private static final int TOP_N = 3;

    /**
     * 根据用户消息检索相关知识片段
     * @param userMessage 用户消息
     * @return 命中的文档内容列表（按相关度排序），没有命中返回空列表
     */
    public List<String> retrieve(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }

        Map<String, String> documents = knowledgeBase.getAllDocuments();
        if (documents.isEmpty()) {
            return List.of();
        }

        // 提取用户消息中的关键词（简单按非字母数字字符分割，过滤掉太短的词）
        List<String> keywords = extractKeywords(userMessage);

        // 统计每个文档的命中次数
        List<DocScore> scored = new ArrayList<>();
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docName = entry.getKey();
            String content = entry.getValue();
            int score = 0;

            // 关键词在文档内容中出现的次数
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    score += countOccurrences(content, keyword);
                }
            }
            // 文档名中包含关键词也加分
            for (String keyword : keywords) {
                if (docName.contains(keyword)) {
                    score += 5; // 文档名命中权重更高
                }
            }

            if (score > 0) {
                scored.add(new DocScore(docName, content, score));
            }
        }

        // 按分数降序排序，取 top N
        scored.sort(Comparator.comparingInt(DocScore::score).reversed());

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_N, scored.size()); i++) {
            result.add(scored.get(i).content());
        }

        System.out.println("[KeywordRetriever] 用户消息命中 " + result.size() + " 个文档");
        return result;
    }

    /**
     * 从用户消息中提取关键词
     * 极简版：按空格和标点分割，保留长度 >= 2 的词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        // 按非中文、非字母数字字符分割
        String[] parts = text.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
        for (String part : parts) {
            if (part.length() >= 2) {
                keywords.add(part);
            }
        }
        // 如果分割后没有有效词（比如全是单字），把原文整体作为一个关键词
        if (keywords.isEmpty() && text.length() >= 2) {
            keywords.add(text.trim());
        }
        return keywords;
    }

    /**
     * 统计子串出现次数
     */
    private int countOccurrences(String text, String keyword) {
        if (keyword == null || keyword.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    /**
     * 文档评分记录
     */
    private record DocScore(String name, String content, int score) {}
}
