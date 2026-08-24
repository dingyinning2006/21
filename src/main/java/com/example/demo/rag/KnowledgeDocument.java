package com.example.demo.rag;

import java.util.List;

/**
 * 知识文档
 * RAG知识库中的一条文档，包含标题、内容和用于检索的关键词
 */
public class KnowledgeDocument {

    private String id;
    private String title;
    private String content;
    private List<String> keywords;

    public KnowledgeDocument() {
    }

    public KnowledgeDocument(String id, String title, String content, List<String> keywords) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.keywords = keywords;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    /**
     * 判断用户消息是否命中文档的关键词
     */
    public boolean matches(String userMessage) {
        if (userMessage == null || userMessage.isBlank() || keywords == null) {
            return false;
        }
        String lower = userMessage.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
