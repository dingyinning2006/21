package com.example.demo.agent.contract.m5.template;

import com.example.demo.agent.contract.m5.model.TaskCategory;
import com.example.demo.agent.contract.m5.model.TaskDifficulty;
import com.example.demo.agent.contract.m5.model.TaskTemplate;
import com.example.demo.agent.contract.m5.model.TimeOfDay;

import java.util.List;

/**
 * 拖延启动类任务模板。
 *
 * <p>对应压力源：拖延行为。
 * 核心思路：降低启动门槛，用"两分钟法则"和"小步启动"打破拖延循环。
 */
public final class ProcrastinationTemplates {

    private ProcrastinationTemplates() {}

    public static final TaskTemplate TWO_MINUTE_START = new TaskTemplate(
            "PROCRAST_001",
            TaskCategory.PROCRASTINATION_START,
            "两分钟启动法",
            "选定一件一直拖延的事，只做两分钟，允许两分钟后停止——目标是打破'开始'的阻力",
            10,
            TaskDifficulty.EASY,
            TimeOfDay.MORNING,
            List.of(
                    "1. 从待办清单中选一件最想拖延但又必须做的事",
                    "2. 设定一个2分钟的计时器",
                    "3. 告诉自己：只做2分钟，到点就可以停",
                    "4. 开始做这件事的第一步（打开文档/拿出书/写第一行）",
                    "5. 2分钟到了，如果想停就停；如果进入状态了，可以继续",
                    "6. 记录：做了什么？2分钟后是停了还是继续了？"
            ),
            "完成2分钟启动，并有1句话记录（做了什么+是否继续）",
            "只做第1-4步：选一件事，设定计时器，开始做第一步，不记录",
            List.of("两分钟法则", "启动", "破局", "拖延")
    );

    public static final TaskTemplate TASK_BREAKDOWN = new TaskTemplate(
            "PROCRAST_002",
            TaskCategory.PROCRASTINATION_START,
            "拆解一个拖延的大任务",
            "把一件因'太大了不知道从哪开始'而拖延的任务，拆成5个以上15分钟内能完成的小步骤",
            20,
            TaskDifficulty.MEDIUM,
            TimeOfDay.MORNING,
            List.of(
                    "1. 写下那件一直拖延的大任务名称（如'写论文'/'准备考试'）",
                    "2. 问自己：这件事的第一步具体是什么？写下来",
                    "3. 继续拆解：第一步之后呢？再之后呢？至少拆出5个步骤",
                    "4. 对每个步骤估算时间，确保每个不超过15-20分钟",
                    "5. 如果某个步骤超过20分钟，继续拆",
                    "6. 给步骤排序，标出今天可以做的第一步"
            ),
            "一个大任务被拆成至少5个步骤，每个步骤不超过20分钟，并标出了今天可做的第一步",
            "只拆出3个步骤，不估算时间，不标今天的第一步",
            List.of("任务拆解", "WBS", "降低门槛", "拖延")
    );

    public static final TaskTemplate POMODORO_SESSION = new TaskTemplate(
            "PROCRAST_003",
            TaskCategory.PROCRASTINATION_START,
            "完成一个番茄钟专注",
            "用25分钟专注+5分钟休息的番茄钟，完成一段专注工作，期间不碰手机",
            30,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 选定一件要专注做的事，明确这25分钟要完成什么",
                    "2. 手机调到静音并放到视线外，关闭电脑通知",
                    "3. 设定25分钟计时器，开始工作",
                    "4. 如果中途走神或想刷手机，在纸上记下来，然后拉回注意力",
                    "5. 25分钟到了，停笔，设定5分钟休息计时器",
                    "6. 休息时站起来走动、喝水，不刷手机",
                    "7. 记录：这25分钟完成了什么？走神了几次？"
            ),
            "完成一个25分钟专注+5分钟休息的番茄钟，并有简短记录（完成了什么+走神次数）",
            "只做15分钟专注（不休息），不记录",
            List.of("番茄钟", "专注", "时间管理", "拖延")
    );

    public static final TaskTemplate ENVIRONMENT_TIDY = new TaskTemplate(
            "PROCRAST_004",
            TaskCategory.PROCRASTINATION_START,
            "整理工作环境",
            "用10分钟整理书桌/工作区，清除干扰物，为接下来的工作创造一个清爽的环境",
            10,
            TaskDifficulty.EASY,
            TimeOfDay.MORNING,
            List.of(
                    "1. 设定10分钟计时器",
                    "2. 把桌面上与当前任务无关的东西收起来（零食、杂物、其他书本）",
                    "3. 只留下当前任务需要的东西",
                    "4. 把手机放到抽屉或另一个房间",
                    "5. 倒一杯水放在手边",
                    "6. 坐下来，深呼吸3次，准备开始"
            ),
            "桌面只留当前任务相关物品，手机已收走，水杯就位",
            "只把桌面上最明显的杂物收走，不做深度整理",
            List.of("环境整理", "减少干扰", "启动准备")
    );

    public static final TaskTemplate REWARD_AFTER = new TaskTemplate(
            "PROCRAST_005",
            TaskCategory.PROCRASTINATION_START,
            "设定完成后的奖励",
            "为今天要完成的任务设定一个具体的、即时的小奖励，增强完成动力",
            5,
            TaskDifficulty.EASY,
            TimeOfDay.MORNING,
            List.of(
                    "1. 写下今天最想完成的一件事",
                    "2. 想一件完成后可以立刻做的、让自己开心的小事（看一集剧/吃点好吃的/玩15分钟游戏/散步）",
                    "3. 把奖励写在便签上，贴在电脑旁",
                    "4. 告诉自己：做完这件事，就去享受奖励",
                    "5. 完成后，真的去享受奖励，不要有负罪感"
            ),
            "有一张写着'任务+奖励'的便签，完成后真的享受了奖励",
            "只在脑子里想一下奖励是什么，不写便签",
            List.of("奖励", "正反馈", "动力", "拖延")
    );

    public static List<TaskTemplate> all() {
        return List.of(
                TWO_MINUTE_START,
                TASK_BREAKDOWN,
                POMODORO_SESSION,
                ENVIRONMENT_TIDY,
                REWARD_AFTER
        );
    }
}
