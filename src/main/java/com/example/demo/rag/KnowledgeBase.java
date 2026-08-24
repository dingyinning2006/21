package com.example.demo.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库
 * 极简关键词检索版RAG的知识存储，内存中维护文档列表，提供关键词匹配检索
 */
@Component
public class KnowledgeBase {

    private final List<KnowledgeDocument> documents = new ArrayList<>();

    public KnowledgeBase() {
        initDefaultKnowledge();
        System.out.println("RAG知识库初始化完成，共 " + documents.size() + " 篇文档");
    }

    /**
     * 初始化默认知识库
     * 预置项目相关的知识文档，用户问相关问题时可检索到
     */
    private void initDefaultKnowledge() {
        documents.add(new KnowledgeDocument(
                "project-intro",
                "项目介绍",
                """
                这是一个基于微信iLink协议的AI机器人项目，使用Spring Boot框架开发。
                项目支持文本对话、图片生成、语音合成、天气查询、单位换算、BMI计算等功能。
                项目采用三层消息路由架构：Skill关键词触发 → RAG知识检索增强 → LLM兜底闲聊。
                技术栈：Spring Boot 4.0.7 + JDK 21 + 通义千问大模型 + wechat-ilink-sdk。
                """,
                List.of("项目", "介绍", "是什么", "做什么", "架构", "技术栈", "功能")
        ));

        documents.add(new KnowledgeDocument(
                "skill-vs-tool",
                "Skill和Tool的区别",
                """
                Skill和Tool是两种不同的功能触发方式：
                Skill（技能）：基于关键词匹配触发，程序层面直接路由，不经过LLM决策。
                特点是确定性高、响应速度快，适合高频固定功能。
                Tool（工具）：基于Function Calling，由LLM理解用户意图后决定调用哪个工具。
                特点是灵活智能，但依赖LLM判断，可能出现选错或不调用的情况，响应较慢。
                本项目同时支持两种方式，高频功能用Skill，复杂灵活的需求用Tool。
                """,
                List.of("skill", "tool", "区别", "不同", "技能", "工具", "function calling")
        ));

        documents.add(new KnowledgeDocument(
                "rag-intro",
                "RAG检索增强生成",
                """
                RAG（Retrieval-Augmented Generation）即检索增强生成。
                工作原理：先从知识库中检索相关文档，然后把检索到的内容加入Prompt，让LLM基于检索到的知识回答问题。
                为什么需要RAG：1.LLM知识有截止日期，不知道最新信息；2.LLM不知道私有数据；
                3.微调成本高更新慢，RAG只需更新知识库；4.减少幻觉，回答有依据。
                本项目实现的是极简关键词检索版RAG，通过关键词匹配知识库文档，然后增强Prompt。
                """,
                List.of("rag", "检索", "增强", "知识库", "原理", "是什么", "检索增强")
        ));

        documents.add(new KnowledgeDocument(
                "usage-guide",
                "使用说明",
                """
                微信AI机器人使用说明：
                1. 智能对话：直接发文字，AI会回复你
                2. 图片生成：发送"画xxx"，例如"画一只可爱的猫咪"
                3. 语音回复：发送"说xxx"，例如"说你好世界"，会回复语音文件
                4. 天气查询：发送"南京天气"、"北京气温"查询实时天气
                5. 单位换算：直接问"5公里等于多少英里"
                6. 帮助功能：发送"帮助"、"功能"查看完整功能列表
                7. 每日一句：发送"每日一句"、"名言"获取励志语录
                """,
                List.of("使用", "说明", "怎么用", "教程", "操作", "指南")
        ));

        documents.add(new KnowledgeDocument(
                "weather-api",
                "天气查询说明",
                """
                天气查询功能使用WeatherAPI提供天气数据。
                支持查询全球城市的实时天气，包括温度、体感温度、天气状况、风力、湿度等。
                使用方式：直接发送城市名+天气，例如"南京天气"、"北京今天气温"、"上海下雨吗"。
                如果未指定城市，默认查询Nanjing（南京）的天气。
                天气数据来源：https://www.weatherapi.com/
                """,
                List.of("天气", "气温", "温度", "weather", "查询天气", "天气预报")
        ));
    }

    /**
     * 根据用户消息检索相关文档（极简关键词匹配）
     *
     * @param userMessage 用户消息
     * @return 匹配到的文档列表，按匹配度排序（这里简单返回所有匹配的）
     */
    public List<KnowledgeDocument> retrieve(String userMessage) {
        List<KnowledgeDocument> matched = new ArrayList<>();
        for (KnowledgeDocument doc : documents) {
            if (doc.matches(userMessage)) {
                matched.add(doc);
            }
        }
        return matched;
    }

    /**
     * 添加文档到知识库
     */
    public void addDocument(KnowledgeDocument doc) {
        documents.add(doc);
    }

    /**
     * 获取所有文档
     */
    public List<KnowledgeDocument> getAllDocuments() {
        return documents;
    }
}
