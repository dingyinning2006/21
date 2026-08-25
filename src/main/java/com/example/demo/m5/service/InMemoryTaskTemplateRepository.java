package com.example.demo.m5.service;

import com.example.demo.m5.model.TaskCategory;
import com.example.demo.m5.model.TaskTemplate;
import com.example.demo.m5.template.BedtimeRelaxationTemplates;
import com.example.demo.m5.template.ExamReviewTemplates;
import com.example.demo.m5.template.InterpersonalTemplates;
import com.example.demo.m5.template.InterviewPrepTemplates;
import com.example.demo.m5.template.ProcrastinationTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存版任务模板仓库。
 *
 * <p>预加载所有五类模板，支持按ID、类别、压力源、难度查询。
 * 用于独立测试和演示，生产环境可替换为数据库实现。
 */
public class InMemoryTaskTemplateRepository implements TaskTemplateRepository {

    private final Map<String, TaskTemplate> templatesById = new ConcurrentHashMap<>();

    public InMemoryTaskTemplateRepository() {
        loadAll();
    }

    private void loadAll() {
        List<TaskTemplate> all = new ArrayList<>();
        all.addAll(InterviewPrepTemplates.all());
        all.addAll(ExamReviewTemplates.all());
        all.addAll(ProcrastinationTemplates.all());
        all.addAll(InterpersonalTemplates.all());
        all.addAll(BedtimeRelaxationTemplates.all());

        for (TaskTemplate t : all) {
            templatesById.put(t.templateId(), t);
        }
    }

    @Override
    public Optional<TaskTemplate> findById(String templateId) {
        return Optional.ofNullable(templatesById.get(templateId));
    }

    @Override
    public List<TaskTemplate> findByCategory(TaskCategory category) {
        return templatesById.values().stream()
                .filter(t -> t.category() == category)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskTemplate> findByStressSource(String stressSource) {
        return templatesById.values().stream()
                .filter(t -> t.matchesStressSource(stressSource))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskTemplate> findAll() {
        return new ArrayList<>(templatesById.values());
    }

    @Override
    public List<TaskTemplate> findByCategoryAndMaxDifficulty(TaskCategory category, int maxDifficultyLevel) {
        return templatesById.values().stream()
                .filter(t -> t.category() == category)
                .filter(t -> t.difficulty().getLevel() <= maxDifficultyLevel)
                .collect(Collectors.toList());
    }

    /**
     * 获取模板总数。
     */
    public int size() {
        return templatesById.size();
    }
}
