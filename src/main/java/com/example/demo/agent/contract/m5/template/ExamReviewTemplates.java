package com.example.demo.agent.contract.m5.template;

import com.example.demo.agent.contract.m5.model.TaskCategory;
import com.example.demo.agent.contract.m5.model.TaskDifficulty;
import com.example.demo.agent.contract.m5.model.TaskTemplate;
import com.example.demo.agent.contract.m5.model.TimeOfDay;

import java.util.List;

/**
 * 考试复习类任务模板。
 *
 * <p>对应压力源：考试焦虑。
 * 每个模板粒度控制在当天可执行，避免"复习全书"这类不可验收任务。
 */
public final class ExamReviewTemplates {

    private ExamReviewTemplates() {}

    public static final TaskTemplate REVIEW_PLAN = new TaskTemplate(
            "EXAM_001",
            TaskCategory.EXAM_REVIEW,
            "制定复习计划",
            "梳理考试范围和自身掌握程度，排出未来几天的复习优先级和每日任务量",
            25,
            TaskDifficulty.MEDIUM,
            TimeOfDay.MORNING,
            List.of(
                    "1. 拿出考试大纲/目录/课件，列出所有章节和知识点",
                    "2. 对每个知识点标注掌握程度：熟练/一般/不会（用符号标记）",
                    "3. 统计'不会'和'一般'的知识点数量，估算总复习时间",
                    "4. 按优先级排序：分值高且不会的 > 分值高且一般的 > 分值低且不会的",
                    "5. 把任务分配到剩余天数，每天不超过3个重点知识点"
            ),
            "有一份复习计划表：所有知识点标注了掌握程度，按优先级排序并分配到每天",
            "只做第1-2步：列出章节并标注掌握程度，不做详细分配",
            List.of("复习计划", "考试", "优先级", "自我评估")
    );

    public static final TaskTemplate STUDY_ONE_TOPIC = new TaskTemplate(
            "EXAM_002",
            TaskCategory.EXAM_REVIEW,
            "攻克一个重点知识点",
            "针对一个'不会'或'一般'的重点知识点，完成理解+笔记+做题的完整学习闭环",
            40,
            TaskDifficulty.HARD,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 选定一个今天要攻克的知识点（从复习计划的优先级顶部取）",
                    "2. 重读课件/教材相关章节，用自己的话写出核心概念（不超过200字）",
                    "3. 做3-5道相关练习题，错题标注原因（概念不清/计算失误/没见过）",
                    "4. 把错题和核心概念整理到错题本/笔记中",
                    "5. 合上书本，用1分钟给自己讲一遍这个知识点，能讲清楚就算过关"
            ),
            "一个知识点有200字以内的核心概念笔记 + 3-5道练习题（含错题标注），能脱稿讲清楚",
            "只做第2步：重读课件并写出核心概念（200字以内），不做题",
            List.of("知识点", "深度学习", "做题", "错题")
    );

    public static final TaskTemplate PRACTICE_SET = new TaskTemplate(
            "EXAM_003",
            TaskCategory.EXAM_REVIEW,
            "完成一套练习题",
            "在规定时间内完成一套练习题或真题，并批改和整理错题",
            50,
            TaskDifficulty.HARD,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 选一套练习题/真题/模拟卷，设定与考试相同的时间限制",
                    "2. 计时做题，中途不翻书，模拟真实考试状态",
                    "3. 时间到后停笔，对照答案批改",
                    "4. 统计正确率，标记每道错题的错误类型",
                    "5. 把错题整理到错题本，写出正确思路和自己错在哪里"
            ),
            "完成一套计时练习，有批改结果和错题整理（含错误原因分析）",
            "只做半套题（不计时），批改后只标记错题不整理",
            List.of("刷题", "真题", "模拟考试", "错题")
    );

    public static final TaskTemplate ERROR_REVIEW = new TaskTemplate(
            "EXAM_004",
            TaskCategory.EXAM_REVIEW,
            "回顾错题本",
            "重新做一遍错题本中的题目，确认之前的薄弱点已经掌握",
            30,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 从错题本中选出10-15道代表性错题（覆盖不同知识点和错误类型）",
                    "2. 不看答案，重新做一遍",
                    "3. 对照答案，标记仍然做错的题",
                    "4. 对仍然做错的题，重新看相关知识点，写出正确思路",
                    "5. 统计二次正确率，评估薄弱点是否改善"
            ),
            "重做10-15道错题，有二次正确率统计，仍然做错的题有重新整理的思路",
            "只快速浏览错题本，看5道题的正确思路，不动手重做",
            List.of("错题", "查漏补缺", "复习")
    );

    public static final TaskTemplate FRAMEWORK_REVIEW = new TaskTemplate(
            "EXAM_005",
            TaskCategory.EXAM_REVIEW,
            "梳理知识框架",
            "用思维导图或大纲形式梳理整门课的知识框架，建立全局视野，不学习新内容",
            25,
            TaskDifficulty.EASY,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 拿出一张纸或打开思维导图工具",
                    "2. 不看书，凭记忆画出整门课的章节结构和核心知识点",
                    "3. 画完后翻书对照，补上遗漏的重要知识点",
                    "4. 用不同颜色标记：熟练的/还需要看的",
                    "5. 把'还需要看的'快速过一遍，不深究"
            ),
            "有一份凭记忆画出的知识框架图（已对照书本补全），标注了掌握程度",
            "只翻书看一遍目录和每章标题，在脑子里过一遍，不动笔画图",
            List.of("知识框架", "思维导图", "考前复习", "全局梳理")
    );

    public static List<TaskTemplate> all() {
        return List.of(
                REVIEW_PLAN,
                STUDY_ONE_TOPIC,
                PRACTICE_SET,
                ERROR_REVIEW,
                FRAMEWORK_REVIEW
        );
    }
}
