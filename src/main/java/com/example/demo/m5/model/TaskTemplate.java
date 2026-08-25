package com.example.demo.m5.model;

import java.util.List;

/**
 * 现实任务模板。
 *
 * <p>M5-001 验收要求：每个模板必须包含目标、预计时长、完成标准和失败后的缩小版本。
 * 任务粒度控制在当天可执行，避免"准备面试"这类不可验收任务。
 * 模板不包含诊断、药物或强制性承诺。
 *
 * @param templateId      模板唯一标识，如 "INTERVIEW_001"
 * @param category        任务类别
 * @param title           任务标题，简短明确
 * @param goal            任务目标：做完这件事能达成什么
 * @param estimatedMinutes 预计时长（分钟），当天可完成
 * @param difficulty      难度等级
 * @param suggestedTime   建议执行时段
 * @param steps           具体执行步骤，按顺序排列
 * @param completionCriteria 完成标准：如何判断任务已完成（可验收）
 * @param fallbackVersion 失败后的缩小版本：如果状态不好，做这个简化版也算完成
 * @param tags            标签，用于检索和匹配
 */
public record TaskTemplate(
        String templateId,
        TaskCategory category,
        String title,
        String goal,
        int estimatedMinutes,
        TaskDifficulty difficulty,
        TimeOfDay suggestedTime,
        List<String> steps,
        String completionCriteria,
        String fallbackVersion,
        List<String> tags
) {
    public TaskTemplate {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId 不能为空");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title 不能为空");
        }
        if (estimatedMinutes <= 0 || estimatedMinutes > 120) {
            throw new IllegalArgumentException("预计时长必须在 1-120 分钟之间，当前: " + estimatedMinutes);
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("steps 不能为空");
        }
        if (completionCriteria == null || completionCriteria.isBlank()) {
            throw new IllegalArgumentException("completionCriteria 不能为空");
        }
        if (fallbackVersion == null || fallbackVersion.isBlank()) {
            throw new IllegalArgumentException("fallbackVersion 不能为空");
        }
    }

    /**
     * 获取缩小版本的预计时长（取原版的一半，至少5分钟）。
     */
    public int getFallbackMinutes() {
        return Math.max(5, estimatedMinutes / 2);
    }

    /**
     * 判断该模板是否匹配给定的压力源关键词。
     */
    public boolean matchesStressSource(String stressSource) {
        if (stressSource == null || stressSource.isBlank()) {
            return false;
        }
        String lower = stressSource.toLowerCase();
        if (category.getStressSource().toLowerCase().contains(lower)
                || lower.contains(category.getStressSource().toLowerCase())) {
            return true;
        }
        return tags != null && tags.stream().anyMatch(t -> t.toLowerCase().contains(lower));
    }
}
