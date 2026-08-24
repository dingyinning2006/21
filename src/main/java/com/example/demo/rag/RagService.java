package com.example.demo.rag;

import com.example.demo.llm.LlmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 服务：检索知识库 → 增强 Prompt → 调用 LLM 生成回答
 * 带开关控制，可运行时开启/关闭
 */
@Service
public class RagService {

    @Autowired
    private KeywordRetriever retriever;

    @Autowired
    private LlmService llmService;

    // RAG 开关（默认开启）
    private volatile boolean enabled = true;

    /**
     * 开启 RAG
     */
    public void enable() {
        this.enabled = true;
        System.out.println("[RagService] RAG 已开启");
    }

    /**
     * 关闭 RAG
     */
    public void disable() {
        this.enabled = false;
        System.out.println("[RagService] RAG 已关闭");
    }

    /**
     * 查询 RAG 是否开启
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 尝试用 RAG 回答用户问题
     * @param userMessage 用户消息
     * @return RAG 增强后的 LLM 回答；RAG 关闭或未命中知识库返回 null
     */
    public String answer(String userMessage) {
        // 1. 检查开关
        if (!enabled) {
            System.out.println("[RagService] RAG 已关闭，跳过");
            return null;
        }

        // 2. 检索知识库
        List<String> relevantDocs = retriever.retrieve(userMessage);
        if (relevantDocs.isEmpty()) {
            System.out.println("[RagService] 知识库未命中相关内容");
            return null;
        }

        // 3. 构建增强 Prompt
        String enhancedPrompt = buildEnhancedPrompt(userMessage, relevantDocs);
        System.out.println("[RagService] 已增强 Prompt，命中 " + relevantDocs.size() + " 个文档");

        // 4. 调用 LLM
        try {
            return llmService.chat(enhancedPrompt);
        } catch (Exception e) {
            System.err.println("[RagService] LLM 调用失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 构建 RAG 增强 Prompt
     * 把检索到的知识片段作为参考资料拼到用户问题前面
     */
    private String buildEnhancedPrompt(String userMessage, List<String> relevantDocs) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下参考资料回答用户的问题。如果参考资料中没有相关信息，请如实说不知道，不要编造。\n\n");
        sb.append("【参考资料】\n");
        for (int i = 0; i < relevantDocs.size(); i++) {
            sb.append(i + 1).append(". ").append(relevantDocs.get(i)).append("\n\n");
        }
        sb.append("【用户问题】\n").append(userMessage);
        return sb.toString();
    }
}
