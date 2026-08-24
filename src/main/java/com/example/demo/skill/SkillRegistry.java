package com.example.demo.skill;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Skill 注册表
 * 管理所有已注册的Skill，提供关键词匹配查找
 */
@Component
public class SkillRegistry {

    private final List<Skill> skills;

    public SkillRegistry(List<Skill> skills) {
        this.skills = skills;
        System.out.println("已注册 " + skills.size() + " 个Skill：");
        for (Skill skill : skills) {
            System.out.println("  - " + skill.getName() + ": " + skill.getDescription()
                    + "，关键词：" + skill.getKeywords());
        }
    }

    /**
     * 根据用户消息匹配命中的Skill
     *
     * @param userMessage 用户消息
     * @return 命中的Skill，没有则返回empty
     */
    public Optional<Skill> match(String userMessage) {
        for (Skill skill : skills) {
            if (skill.matches(userMessage)) {
                return Optional.of(skill);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取所有已注册的Skill
     */
    public List<Skill> getAllSkills() {
        return skills;
    }
}
