package com.example.demo.skill;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillKeywordRouter {

    private final List<Keyword> skills;

    public SkillKeywordRouter(List<Keyword> skills) {
        this.skills = skills;
    }

    public String route(String userText) {
        for (Keyword skill : skills) {
            if (skill.matches(userText)) {
                System.out.println("命中 Skill：" + skill.getName());

                String result = skill.executeText(userText);

                System.out.println("Skill 结果：" + result);
                return result;
            }
        }

        return null;
    }
}
