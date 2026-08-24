package com.example.demo.skill;

import java.util.List;

/**
 * Skill 接口：关键词触发的确定性技能
 * 与 tool 包下的 Tool（LLM function calling 驱动）不同，
 * Skill 是通过关键词直接匹配触发，不经过 LLM，执行结果确定。
 */
public interface Skill {

    /**
     * 返回该 Skill 的触发关键词列表
     * 用户消息中包含任意一个关键词即命中
     */
    List<String> getKeywords();

    /**
     * 命中后执行的逻辑
     * @param userMessage 用户原始消息
     * @return 要回复给用户的文本
     */
    String execute(String userMessage);
}
