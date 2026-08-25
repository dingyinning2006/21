package com.example.demo.m5.service;

import com.example.demo.m5.model.TaskCategory;
import com.example.demo.m5.model.TaskTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 任务模板仓库接口。
 *
 * <p>抽象模板的存储和查询，M5 内部使用内存实现，
 * 集成到主项目时可替换为数据库或配置文件实现。
 */
public interface TaskTemplateRepository {

    /**
     * 根据模板ID查询。
     */
    Optional<TaskTemplate> findById(String templateId);

    /**
     * 查询某一类别的所有模板。
     */
    List<TaskTemplate> findByCategory(TaskCategory category);

    /**
     * 根据压力源关键词匹配模板。
     */
    List<TaskTemplate> findByStressSource(String stressSource);

    /**
     * 查询所有模板。
     */
    List<TaskTemplate> findAll();

    /**
     * 根据类别和难度筛选。
     */
    List<TaskTemplate> findByCategoryAndMaxDifficulty(TaskCategory category, int maxDifficultyLevel);
}
