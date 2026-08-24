package com.example.demo.skill;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skill 路由：遍历所有 Skill，用关键词匹配判断命中哪个
 * 命中则执行该 Skill 并返回结果；都没命中返回 null
 */
@Service
public class SkillRouter {

    // Spring 自动注入所有实现了 Skill 接口的 Bean
    @Autowired
    private List<Skill> skills;

    /**
     * 尝试用 Skill 处理用户消息
     * @param userMessage 用户原始消息
     * @return 命中 Skill 的执行结果；都没命中返回 null
     */
    public String route(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }

        for (Skill skill : skills) {
            for (String keyword : skill.getKeywords()) {
                if (userMessage.contains(keyword)) {
                    System.out.println("[SkillRouter] 命中 Skill: "
                            + skill.getClass().getSimpleName()
                            + "，关键词: " + keyword);
                    return skill.execute(userMessage);
                }
            }
        }

        System.out.println("[SkillRouter] 未命中任何 Skill");
        return null;
    }
}
