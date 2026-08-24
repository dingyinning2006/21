package com.example.demo.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG服务
 * 极简关键词检索版RAG，提供知识检索和Prompt增强功能
 * 支持通过配置开启/关闭RAG，用于对比测试
 */
@Service
public class RagService {

    private final KnowledgeBase knowledgeBase;

    @Value("${rag.enabled:true}")
    private boolean enabled;

    public RagService(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 判断用户消息是否命中RAG（即是否检索到相关知识）
     *
     * @param userMessage 用户消息
     * @return true表示命中RAG，需要增强Prompt
     */
    public boolean isHit(String userMessage) {
        if (!enabled) {
            return false;
        }
        List<KnowledgeDocument> docs = knowledgeBase.retrieve(userMessage);
        return !docs.isEmpty();
    }

    /**
     * 检索相关知识并生成增强后的System Prompt
     *
     * @param userMessage 用户消息
     * @param originalSystemPrompt 原始的System Prompt
     * @return 增强后的System Prompt
     */
    public String enhancePrompt(String userMessage, String originalSystemPrompt) {
        if (!enabled) {
            return originalSystemPrompt;
        }

        List<KnowledgeDocument> docs = knowledgeBase.retrieve(userMessage);
        if (docs.isEmpty()) {
            return originalSystemPrompt;
        }

        // 构建知识上下文
        StringBuilder knowledgeContext = new StringBuilder();
        knowledgeContext.append("\n\n【知识库检索结果】\n");
        knowledgeContext.append("以下是从知识库中检索到的相关信息，请基于这些信息回答用户问题，如果信息不足可以补充你的知识：\n\n");

        for (int i = 0; i < docs.size(); i++) {
            KnowledgeDocument doc = docs.get(i);
            knowledgeContext.append("--- 文档").append(i + 1).append("：").append(doc.getTitle()).append(" ---\n");
            knowledgeContext.append(doc.getContent()).append("\n\n");
        }

        knowledgeContext.append("【检索结束】\n");
        knowledgeContext.append("请结合以上知识库内容回答用户，回答时可以引用知识库中的信息。\n");

        System.out.println("RAG命中，检索到 " + docs.size() + " 篇文档，增强Prompt");

        return originalSystemPrompt + knowledgeContext.toString();
    }

    /**
     * 获取检索到的文档（用于调试和对比测试）
     */
    public List<KnowledgeDocument> retrieve(String userMessage) {
        return knowledgeBase.retrieve(userMessage);
    }

    /**
     * RAG是否开启
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 开启RAG
     */
    public void enable() {
        this.enabled = true;
        System.out.println("RAG已开启");
    }

    /**
     * 关闭RAG
     */
    public void disable() {
        this.enabled = false;
        System.out.println("RAG已关闭");
    }
}
