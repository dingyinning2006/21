package com.example.demo.m5.template;

import com.example.demo.m5.model.TaskCategory;
import com.example.demo.m5.model.TaskDifficulty;
import com.example.demo.m5.model.TaskTemplate;
import com.example.demo.m5.model.TimeOfDay;

import java.util.List;

/**
 * 面试准备类任务模板。
 *
 * <p>对应压力源：求职压力。
 * 每个模板粒度控制在当天可执行，包含目标、时长、完成标准和缩小版本。
 */
public final class InterviewPrepTemplates {

    private InterviewPrepTemplates() {}

    /**
     * 梳理简历亮点。
     * 适合面试前7-5天，基础准备阶段。
     */
    public static final TaskTemplate RESUME_HIGHLIGHTS = new TaskTemplate(
            "INTERVIEW_001",
            TaskCategory.INTERVIEW_PREP,
            "梳理简历亮点",
            "从简历中提炼3个最能匹配岗位的经历亮点，准备好用1分钟讲清楚每个亮点",
            30,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 拿出简历和岗位JD，逐行对比",
                    "2. 选出3个最匹配的经历（项目/实习/竞赛）",
                    "3. 每个经历用STAR框架写3句话：情境-任务-行动-结果",
                    "4. 大声朗读一遍，计时确保每个1分钟内能讲完"
            ),
            "产出3条STAR格式的经历亮点笔记，每条能在1分钟内讲完",
            "只做第1-2步：对比简历和JD，圈出3个最相关的经历即可，不写STAR框架",
            List.of("简历", "求职", "面试准备", "自我梳理")
    );

    /**
     * 准备自我介绍。
     * 适合面试前6-4天。
     */
    public static final TaskTemplate SELF_INTRODUCTION = new TaskTemplate(
            "INTERVIEW_002",
            TaskCategory.INTERVIEW_PREP,
            "准备1分钟自我介绍",
            "写出并练习一段1分钟的自我介绍，涵盖背景、核心优势和求职动机",
            25,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 写初稿：1句开场+2句背景+2句核心优势+1句动机，控制在150字以内",
                    "2. 对着镜子或手机录音朗读3遍",
                    "3. 回听录音，调整语速和停顿，去掉口头禅",
                    "4. 脱稿练习2遍，确保能自然讲出来"
            ),
            "有一份150字以内的自我介绍稿，能脱稿在1分钟内自然讲完",
            "只写初稿（150字以内），不录音不练习",
            List.of("自我介绍", "面试", "表达练习")
    );

    /**
     * 模拟常见面试问题。
     * 适合面试前5-3天。
     */
    public static final TaskTemplate COMMON_QUESTIONS = new TaskTemplate(
            "INTERVIEW_003",
            TaskCategory.INTERVIEW_PREP,
            "准备3个常见面试问题的回答",
            "针对3个高频面试问题（最大优势/最大挑战/为什么选我们）写出答题要点并口头练习",
            35,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 从以下问题中选3个：最大优势、最大挑战、为什么选这个岗位、职业规划、缺点",
                    "2. 每个问题写3-5个答题要点（不是逐字稿）",
                    "3. 对着手机录音，每个问题用要点回答一遍（2分钟内）",
                    "4. 回听，标记需要改进的地方"
            ),
            "3个问题各有3-5条答题要点，且每个都录音练习过一遍",
            "只写3个问题的答题要点，不录音练习",
            List.of("面试问题", "模拟面试", "答题准备")
    );

    /**
     * 研究公司和岗位。
     * 适合面试前4-2天。
     */
    public static final TaskTemplate COMPANY_RESEARCH = new TaskTemplate(
            "INTERVIEW_004",
            TaskCategory.INTERVIEW_PREP,
            "研究公司背景和岗位要求",
            "收集目标公司的业务、产品、文化和岗位核心要求，整理成一页笔记",
            30,
            TaskDifficulty.EASY,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 访问公司官网，记录主营业务、最新产品/项目、公司价值观",
                    "2. 搜索公司近期新闻（融资、产品发布、行业动态）",
                    "3. 重新读岗位JD，列出3个核心技能要求和2个软技能要求",
                    "4. 整理成一页笔记：公司概况+岗位要求+我的匹配点"
            ),
            "有一页公司研究笔记，包含公司业务、近期动态、岗位核心要求和个人匹配点",
            "只做第1和第3步：看官网主营业务 + 读JD列3个核心要求",
            List.of("公司研究", "岗位分析", "面试准备")
    );

    /**
     * 完整模拟面试。
     * 适合面试前2-1天。
     */
    public static final TaskTemplate MOCK_INTERVIEW = new TaskTemplate(
            "INTERVIEW_005",
            TaskCategory.INTERVIEW_PREP,
            "完整模拟面试",
            "进行一次30分钟的完整模拟面试（自我介绍+5个问题+反问环节），并复盘改进点",
            45,
            TaskDifficulty.HARD,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 找朋友/同学当面试官，或自己对着镜子/手机录像",
                    "2. 按真实流程：1分钟自我介绍 + 5个问题（含1个行为题+1个专业题）+ 反问环节",
                    "3. 全程计时，模拟真实面试节奏",
                    "4. 结束后立即复盘：记录3个做得好的地方和3个需要改进的点",
                    "5. 针对改进点，重新练习一遍"
            ),
            "完成一次30分钟模拟面试，并有复盘笔记（3优点+3改进点）",
            "只做自我介绍+3个问题的简短模拟（15分钟），不写完整复盘",
            List.of("模拟面试", "实战演练", "面试冲刺")
    );

    /**
     * 面试前一天轻量复习。
     * 适合面试前1天，避免过度准备增加焦虑。
     */
    public static final TaskTemplate DAY_BEFORE_REVIEW = new TaskTemplate(
            "INTERVIEW_006",
            TaskCategory.INTERVIEW_PREP,
            "面试前轻量复习",
            "快速回顾准备好的自我介绍和亮点，准备好面试物品，不做新内容的学习",
            20,
            TaskDifficulty.EASY,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 把自我介绍和3个亮点快速过一遍（各说一遍，不超过10分钟）",
                    "2. 准备面试物品：简历打印、证件、笔记本、笔、着装确认",
                    "3. 确认面试时间、地点/链接，提前规划路线或测试设备",
                    "4. 告诉自己：已经准备充分了，今晚好好休息"
            ),
            "回顾完核心内容，面试物品和时间地点已确认，不学习新内容",
            "只确认面试时间地点和物品准备，不做内容回顾",
            List.of("面试前", "轻量复习", "物品准备")
    );

    /**
     * 获取所有面试准备模板。
     */
    public static List<TaskTemplate> all() {
        return List.of(
                RESUME_HIGHLIGHTS,
                SELF_INTRODUCTION,
                COMMON_QUESTIONS,
                COMPANY_RESEARCH,
                MOCK_INTERVIEW,
                DAY_BEFORE_REVIEW
        );
    }
}
