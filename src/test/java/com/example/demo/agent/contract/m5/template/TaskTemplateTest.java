package com.example.demo.agent.contract.m5.template;

import com.example.demo.agent.contract.m5.model.TaskCategory;
import com.example.demo.agent.contract.m5.model.TaskDifficulty;
import com.example.demo.agent.contract.m5.model.TaskTemplate;
import com.example.demo.agent.contract.m5.model.TimeOfDay;
import com.example.demo.agent.contract.m5.service.InMemoryTaskTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5-001 场景任务模板测试。
 */
class TaskTemplateTest {

    private InMemoryTaskTemplateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskTemplateRepository();
    }

    @Test
    @DisplayName("模板总数：五类模板全部加载")
    void allTemplatesLoaded() {
        List<TaskTemplate> all = repository.findAll();
        assertFalse(all.isEmpty(), "模板库不应为空");

        for (TaskCategory category : TaskCategory.values()) {
            List<TaskTemplate> byCategory = repository.findByCategory(category);
            assertFalse(byCategory.isEmpty(), "类别 " + category + " 应有模板");
        }

        System.out.println("模板总数: " + all.size());
        for (TaskCategory category : TaskCategory.values()) {
            System.out.println("  " + category + ": " + repository.findByCategory(category).size() + " 个");
        }
    }

    @Test
    @DisplayName("每个模板都包含目标、时长、完成标准和缩小版本")
    void everyTemplateHasRequiredFields() {
        List<TaskTemplate> all = repository.findAll();

        for (TaskTemplate t : all) {
            assertNotNull(t.goal(), "模板 " + t.templateId() + " 应有目标");
            assertFalse(t.goal().isBlank(), "模板 " + t.templateId() + " 目标不应为空");

            assertTrue(t.estimatedMinutes() > 0 && t.estimatedMinutes() <= 120,
                    "模板 " + t.templateId() + " 时长应在1-120分钟之间，当前: " + t.estimatedMinutes());

            assertNotNull(t.completionCriteria(), "模板 " + t.templateId() + " 应有完成标准");
            assertFalse(t.completionCriteria().isBlank(), "模板 " + t.templateId() + " 完成标准不应为空");

            assertNotNull(t.fallbackVersion(), "模板 " + t.templateId() + " 应有缩小版本");
            assertFalse(t.fallbackVersion().isBlank(), "模板 " + t.templateId() + " 缩小版本不应为空");

            assertNotNull(t.steps(), "模板 " + t.templateId() + " 应有执行步骤");
            assertFalse(t.steps().isEmpty(), "模板 " + t.templateId() + " 步骤不应为空");
        }
    }

    @Test
    @DisplayName("任务粒度：每个任务当天可执行，不超过120分钟")
    void taskGranularityIsDailyExecutable() {
        List<TaskTemplate> all = repository.findAll();

        for (TaskTemplate t : all) {
            assertTrue(t.estimatedMinutes() <= 120,
                    "模板 " + t.templateId() + " (" + t.title() + ") 时长 " + t.estimatedMinutes()
                            + " 分钟超过120分钟，不是当天可执行的粒度");
        }
    }

    @Test
    @DisplayName("内容安全：模板不包含诊断、药物或强制性承诺")
    void templatesDoNotContainForbiddenContent() {
        List<TaskTemplate> all = repository.findAll();

        List<String> forbiddenDiagnosis = List.of("诊断", "抑郁症", "焦虑症", "病症", "确诊");
        List<String> forbiddenMedicine = List.of("药物", "吃药", "处方药", "安眠药", "剂量");
        List<String> forbiddenPromise = List.of("保证治愈", "一定能好", "包治", "彻底解决");

        for (TaskTemplate t : all) {
            String content = t.title() + " " + t.goal() + " " + t.completionCriteria()
                    + " " + t.fallbackVersion() + " " + String.join(" ", t.steps());

            for (String word : forbiddenDiagnosis) {
                assertFalse(content.contains(word),
                        "模板 " + t.templateId() + " 包含诊断相关词: " + word);
            }
            for (String word : forbiddenMedicine) {
                assertFalse(content.contains(word),
                        "模板 " + t.templateId() + " 包含药物相关词: " + word);
            }
            for (String word : forbiddenPromise) {
                assertFalse(content.contains(word),
                        "模板 " + t.templateId() + " 包含强制性承诺: " + word);
            }
        }
    }

    @Test
    @DisplayName("面试准备模板：6个模板覆盖从7天前到前1天的全流程")
    void interviewTemplatesCoverFullTimeline() {
        List<TaskTemplate> interview = repository.findByCategory(TaskCategory.INTERVIEW_PREP);
        assertEquals(6, interview.size(), "面试准备应有6个模板");

        assertTrue(repository.findById("INTERVIEW_001").isPresent(), "应有简历梳理模板");
        assertTrue(repository.findById("INTERVIEW_002").isPresent(), "应有自我介绍模板");
        assertTrue(repository.findById("INTERVIEW_005").isPresent(), "应有模拟面试模板");
        assertTrue(repository.findById("INTERVIEW_006").isPresent(), "应有面试前轻量复习模板");
    }

    @Test
    @DisplayName("睡前放松模板：全部为低唤醒、EASY难度")
    void bedtimeTemplatesAreLowArousal() {
        List<TaskTemplate> bedtime = repository.findByCategory(TaskCategory.BEDTIME_RELAXATION);
        assertFalse(bedtime.isEmpty());

        for (TaskTemplate t : bedtime) {
            assertEquals(TaskDifficulty.EASY, t.difficulty(),
                    "睡前任务 " + t.templateId() + " 应为EASY难度");
            assertEquals(TimeOfDay.BEDTIME, t.suggestedTime(),
                    "睡前任务 " + t.templateId() + " 应建议睡前执行");
            assertTrue(t.estimatedMinutes() <= 20,
                    "睡前任务 " + t.templateId() + " 时长不应超过20分钟");
        }
    }

    @Test
    @DisplayName("压力源匹配：按关键词能匹配到对应类别的模板")
    void stressSourceMatching() {
        assertTrue(repository.findByStressSource("求职").stream()
                        .anyMatch(t -> t.category() == TaskCategory.INTERVIEW_PREP),
                "搜索'求职'应匹配到面试准备模板");

        assertTrue(repository.findByStressSource("考试").stream()
                        .anyMatch(t -> t.category() == TaskCategory.EXAM_REVIEW),
                "搜索'考试'应匹配到考试复习模板");

        assertTrue(repository.findByStressSource("拖延").stream()
                        .anyMatch(t -> t.category() == TaskCategory.PROCRASTINATION_START),
                "搜索'拖延'应匹配到拖延启动模板");

        assertTrue(repository.findByStressSource("人际").stream()
                        .anyMatch(t -> t.category() == TaskCategory.INTERPERSONAL),
                "搜索'人际'应匹配到人际沟通模板");

        assertTrue(repository.findByStressSource("睡眠").stream()
                        .anyMatch(t -> t.category() == TaskCategory.BEDTIME_RELAXATION),
                "搜索'睡眠'应匹配到睡前放松模板");
    }
}
