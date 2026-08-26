package com.example.demo.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M2-002 调适 Skill 单元测试
 * 验证：关键词匹配、执行结果、参数校验、错误处理
 */
class M2SkillsTest {

    // ==================== BreathingSkill 测试 ====================

    @Test
    void breathingSkillShouldMatchKeywords() {
        BreathingSkill skill = new BreathingSkill();
        assertTrue(skill.matches("我很焦虑"));
        assertTrue(skill.matches("教我深呼吸"));
        assertTrue(skill.matches("4-7-8呼吸法"));
        assertTrue(skill.matches("紧张心慌"));
        assertFalse(skill.matches("今天天气真好"));
        assertFalse(skill.matches(""));
        assertFalse(skill.matches(null));
    }

    @Test
    void breathingSkillShouldReturnGuideFor478() {
        BreathingSkill skill = new BreathingSkill();
        String result = skill.executeText("教我4-7-8呼吸法");
        assertTrue(result.contains("4-7-8"), "应返回4-7-8呼吸引导");
        assertTrue(result.contains("吸气"), "应包含吸气步骤");
        assertTrue(result.contains("呼气"), "应包含呼气步骤");
    }

    @Test
    void breathingSkillShouldReturnGuideForBellyBreathing() {
        BreathingSkill skill = new BreathingSkill();
        String result = skill.executeText("腹式呼吸怎么做");
        assertTrue(result.contains("腹式呼吸"), "应返回腹式呼吸引导");
        assertTrue(result.contains("腹部"), "应包含腹部动作说明");
    }

    @Test
    void breathingSkillShouldDefaultTo478() {
        BreathingSkill skill = new BreathingSkill();
        String result = skill.executeText("我很焦虑");
        assertTrue(result.contains("4-7-8"), "默认应推荐4-7-8呼吸法");
    }

    @Test
    void breathingSkillShouldHandleBlankInput() {
        BreathingSkill skill = new BreathingSkill();
        String result = skill.executeText("");
        assertTrue(result.contains("请告诉我"), "空输入应提示用户选择模式");
    }

    @Test
    void breathingSkillExecuteShouldValidateMode() {
        BreathingSkill skill = new BreathingSkill();
        String result = skill.execute("{\"mode\":\"invalid\"}");
        assertTrue(result.contains("失败"), "无效模式应返回失败");
        result = skill.execute("{\"rounds\":5}");
        assertTrue(result.contains("失败"), "缺少mode应返回失败");
        result = skill.execute("{\"mode\":\"478\",\"rounds\":5}");
        assertTrue(result.contains("4-7-8"), "有效参数应返回引导");
        assertTrue(result.contains("5"), "应包含指定的轮数");
    }

    @Test
    void breathingSkillExecuteShouldValidateRoundsRange() {
        BreathingSkill skill = new BreathingSkill();
        String result = skill.execute("{\"mode\":\"478\",\"rounds\":100}");
        assertTrue(result.contains("失败"), "rounds超出范围应返回失败");
    }

    @Test
    void breathingSkillShouldHaveRequiredParameters() {
        BreathingSkill skill = new BreathingSkill();
        assertTrue(skill.getRequiredParameters().contains("mode"));
        assertNotNull(skill.getParameters());
        assertEquals("breathing_exercise", skill.getName());
    }

    // ==================== SleepRelaxationSkill 测试 ====================

    @Test
    void sleepSkillShouldMatchKeywords() {
        SleepRelaxationSkill skill = new SleepRelaxationSkill();
        assertTrue(skill.matches("我失眠了"));
        assertTrue(skill.matches("睡不着"));
        assertTrue(skill.matches("入睡困难"));
        assertTrue(skill.matches("睡前放松"));
        assertFalse(skill.matches("我很焦虑"));
        assertFalse(skill.matches(""));
    }

    @Test
    void sleepSkillShouldReturnMuscleRelaxationGuide() {
        SleepRelaxationSkill skill = new SleepRelaxationSkill();
        String result = skill.executeText("渐进式肌肉放松");
        assertTrue(result.contains("渐进式肌肉放松"), "应返回肌肉放松引导");
        assertTrue(result.contains("脚趾"), "应包含从脚趾开始的步骤");
    }

    @Test
    void sleepSkillShouldReturnBreathingGuide() {
        SleepRelaxationSkill skill = new SleepRelaxationSkill();
        String result = skill.executeText("睡前呼吸");
        assertTrue(result.contains("睡前呼吸"), "应返回睡前呼吸引导");
    }

    @Test
    void sleepSkillShouldDefaultToMuscleRelaxation() {
        SleepRelaxationSkill skill = new SleepRelaxationSkill();
        String result = skill.executeText("我失眠了");
        assertTrue(result.contains("渐进式肌肉放松"), "默认应推荐渐进式肌肉放松");
    }

    @Test
    void sleepSkillShouldSuggestProfessionalHelpForLongTerm() {
        SleepRelaxationSkill skill = new SleepRelaxationSkill();
        String result = skill.executeText("失眠");
        assertTrue(result.contains("咨询专业医生") || result.contains("长期失眠"),
                "应包含对长期失眠建议专业帮助的内容");
    }

    @Test
    void sleepSkillExecuteShouldValidateMode() {
        SleepRelaxationSkill skill = new SleepRelaxationSkill();
        String result = skill.execute("{\"mode\":\"invalid\"}");
        assertTrue(result.contains("失败"), "无效模式应返回失败");
        result = skill.execute("{\"mode\":\"muscle\"}");
        assertTrue(result.contains("渐进式肌肉放松"), "有效模式应返回引导");
    }

    @Test
    void sleepSkillShouldHaveCorrectName() {
        SleepRelaxationSkill skill = new SleepRelaxationSkill();
        assertEquals("sleep_relaxation", skill.getName());
        assertTrue(skill.getRequiredParameters().contains("mode"));
    }

    // ==================== ShortRelaxationSkill 测试 ====================

    @Test
    void shortSkillShouldMatchKeywords() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        assertTrue(skill.matches("压力太大了"));
        assertTrue(skill.matches("我好累"));
        assertTrue(skill.matches("快崩溃了"));
        assertTrue(skill.matches("5分钟放松"));
        assertTrue(skill.matches("喘不过气"));
        assertFalse(skill.matches("今天吃什么"));
        assertFalse(skill.matches(""));
    }

    @Test
    void shortSkillShouldReturnOfficeRelaxation() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        String result = skill.executeText("办公室放松");
        assertTrue(result.contains("办公室"), "应返回办公室放松引导");
        assertTrue(result.contains("5分钟"), "应包含5分钟时间安排");
    }

    @Test
    void shortSkillShouldReturnBodyScan() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        String result = skill.executeText("正念身体扫描");
        assertTrue(result.contains("正念"), "应返回正念扫描引导");
        assertTrue(result.contains("身体扫描"), "应包含身体扫描");
    }

    @Test
    void shortSkillShouldReturnEmotionReset() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        String result = skill.executeText("情绪重置");
        assertTrue(result.contains("情绪重置"), "应返回情绪重置引导");
        assertTrue(result.contains("命名情绪"), "应包含命名情绪步骤");
    }

    @Test
    void shortSkillShouldDefaultToOfficeRelaxation() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        String result = skill.executeText("压力太大了");
        assertTrue(result.contains("办公室"), "默认应推荐办公室放松");
    }

    @Test
    void shortSkillShouldSuggestAdjustmentForLongTermStress() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        String result = skill.executeText("压力大");
        assertTrue(result.contains("调整工作节奏") || result.contains("寻求支持"),
                "应包含对长期压力建议调整或寻求支持");
    }

    @Test
    void shortSkillExecuteShouldValidateMode() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        String result = skill.execute("{\"mode\":\"invalid\"}");
        assertTrue(result.contains("失败"), "无效模式应返回失败");
        result = skill.execute("{\"mode\":\"office\"}");
        assertTrue(result.contains("办公室"), "有效模式应返回引导");
        result = skill.execute("{\"mode\":\"bodyscan\"}");
        assertTrue(result.contains("正念"), "bodyscan模式应返回正念扫描");
        result = skill.execute("{\"mode\":\"emotion\"}");
        assertTrue(result.contains("情绪重置"), "emotion模式应返回情绪重置");
    }

    @Test
    void shortSkillShouldHaveCorrectName() {
        ShortRelaxationSkill skill = new ShortRelaxationSkill();
        assertEquals("short_relaxation", skill.getName());
        assertTrue(skill.getRequiredParameters().contains("mode"));
    }

    // ==================== 集成测试：Skill 都实现了 Keyword 接口 ====================

    @Test
    void allSkillsShouldImplementKeywordInterface() {
        assertTrue(new BreathingSkill() instanceof Keyword);
        assertTrue(new SleepRelaxationSkill() instanceof Keyword);
        assertTrue(new ShortRelaxationSkill() instanceof Keyword);
    }

    @Test
    void allSkillsShouldHaveNonEmptyNameAndDescription() {
        assertNotNull(new BreathingSkill().getName());
        assertFalse(new BreathingSkill().getName().isBlank());
        assertNotNull(new BreathingSkill().getDescription());
        assertFalse(new BreathingSkill().getDescription().isBlank());

        assertNotNull(new SleepRelaxationSkill().getName());
        assertFalse(new SleepRelaxationSkill().getName().isBlank());
        assertNotNull(new SleepRelaxationSkill().getDescription());

        assertNotNull(new ShortRelaxationSkill().getName());
        assertFalse(new ShortRelaxationSkill().getName().isBlank());
        assertNotNull(new ShortRelaxationSkill().getDescription());
    }
}
