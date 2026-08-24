package com.example.demo.skill;

import java.util.List;

/**
 * Skill 技能接口
 * 基于关键词触发的可插拔功能模块，命中关键词直接执行，不走LLM决策
 * 与Tool(Function Calling)的区别：Skill是程序层面关键词匹配，确定性高、响应快
 */
public interface Skill {

    /**
     * 技能名称（英文标识）
     */
    String getName();

    /**
     * 技能描述（说明这个技能干什么用）
     */
    String getDescription();

    /**
     * 触发关键词列表，用户消息包含任意一个关键词即触发
     */
    List<String> getKeywords();

    /**
     * 执行技能，返回回复内容
     *
     * @param userMessage 用户原始消息
     * @return 回复给用户的文本
     */
    String execute(String userMessage);

    /**
     * 判断用户消息是否命中该技能的关键词
     */
    default boolean matches(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String lower = userMessage.toLowerCase();
        for (String keyword : getKeywords()) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
