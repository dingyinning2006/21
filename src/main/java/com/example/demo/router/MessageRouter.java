package com.example.demo.router;

import com.example.demo.llm.LlmService;
import com.example.demo.rag.RagService;
import com.example.demo.service.FunctionCallingService;
import com.example.demo.skill.SkillRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息总路由：按优先级依次尝试
 * 1. Skill 层（关键词触发，确定性执行）
 * 2. RAG 层（检索知识库增强 Prompt，LLM 回答）
 * 3. LLM 兜底（Function Calling，自由闲聊）
 */
@Service
public class MessageRouter {

    @Autowired
    private SkillRouter skillRouter;

    @Autowired
    private RagService ragService;

    @Autowired
    private FunctionCallingService functionCallingService;

    @Autowired
    private LlmService llmService;

    /**
     * 处理用户消息，返回最终回复
     */
    public String route(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "请输入有效内容。";
        }

        // 0. 控制指令：开启/关闭 RAG
        if (userMessage.contains("开启RAG") || userMessage.contains("打开RAG") || userMessage.contains("启用RAG")) {
            ragService.enable();
            return "已开启 RAG 知识库增强，后续回答将基于知识库内容。";
        }
        if (userMessage.contains("关闭RAG") || userMessage.contains("关掉RAG") || userMessage.contains("禁用RAG")) {
            ragService.disable();
            return "已关闭 RAG 知识库增强，后续回答由 AI 自由回复。";
        }
        if (userMessage.contains("RAG状态") || userMessage.contains("rag状态") || userMessage.contains("RAG开关")) {
            return "RAG 当前状态：" + (ragService.isEnabled() ? "已开启" : "已关闭");
        }

        // 1. Skill 层：关键词命中则直接执行
        System.out.println("[MessageRouter] ===== 第1层：尝试 Skill =====");
        String skillResult = skillRouter.route(userMessage);
        if (skillResult != null) {
            System.out.println("[MessageRouter] Skill 命中，直接返回");
            return skillResult;
        }

        // 2. RAG 层：检索知识库增强 Prompt
        System.out.println("[MessageRouter] ===== 第2层：尝试 RAG =====");
        String ragResult = ragService.answer(userMessage);
        if (ragResult != null) {
            System.out.println("[MessageRouter] RAG 命中，返回增强回答");
            return ragResult;
        }

        // 3. LLM 兜底：Function Calling + 自由闲聊
        System.out.println("[MessageRouter] ===== 第3层：LLM 兜底 =====");
        try {
            return functionCallingService.chat(userMessage);
        } catch (Exception e) {
            System.err.println("[MessageRouter] LLM 兜底失败：" + e.getMessage());
            return "抱歉，服务暂时不可用，请稍后再试。";
        }
    }
}
