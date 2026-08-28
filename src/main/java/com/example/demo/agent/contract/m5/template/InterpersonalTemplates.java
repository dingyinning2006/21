package com.example.demo.agent.contract.m5.template;

import com.example.demo.agent.contract.m5.model.TaskCategory;
import com.example.demo.agent.contract.m5.model.TaskDifficulty;
import com.example.demo.agent.contract.m5.model.TaskTemplate;
import com.example.demo.agent.contract.m5.model.TimeOfDay;

import java.util.List;

/**
 * 人际沟通类任务模板。
 *
 * <p>对应压力源：人际压力。
 * 核心思路：用非暴力沟通框架，把模糊的人际困扰转化为可执行的小步骤。
 */
public final class InterpersonalTemplates {

    private InterpersonalTemplates() {}

    public static final TaskTemplate CLARIFY_CONFLICT = new TaskTemplate(
            "INTERPER_001",
            TaskCategory.INTERPERSONAL,
            "梳理人际困扰",
            "把一段让你不舒服的人际关系或冲突，用客观的语言写清楚发生了什么、你的感受和你的需求",
            20,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 写下涉及的人和场景（谁、什么时候、在哪里）",
                    "2. 客观描述发生了什么：只写事实，不写评价（如'他说了XX'而不是'他故意针对我'）",
                    "3. 写下你的感受：当时是什么情绪？（生气/委屈/害怕/尴尬/失望）",
                    "4. 写下你的需求：你希望发生什么？你需要什么？（被尊重/被理解/公平/空间）",
                    "5. 读一遍，确认这是你的真实感受和需求，而不是对对方的指责"
            ),
            "有一段书面梳理：事实描述+感受+需求，且事实部分不包含评价性语言",
            "只写第1-2步：人和场景+客观事实描述，不写感受和需求",
            List.of("冲突梳理", "非暴力沟通", "自我觉察", "人际")
    );

    public static final TaskTemplate PREPARE_CONVERSATION = new TaskTemplate(
            "INTERPER_002",
            TaskCategory.INTERPERSONAL,
            "准备一次艰难沟通",
            "用非暴力沟通框架（观察-感受-需要-请求）准备一次你需要进行但一直回避的沟通",
            25,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 明确这次沟通的目标：你希望沟通后达成什么？",
                    "2. 写'观察'：具体发生了什么事？（不带评价的事实）",
                    "3. 写'感受'：这件事让你有什么感受？（用情绪词，不是想法）",
                    "4. 写'需要'：你的什么需要没有被满足？（尊重/理解/支持/公平/空间）",
                    "5. 写'请求'：你希望对方具体做什么？（用'你愿意...吗'的句式，具体可执行）",
                    "6. 大声读一遍，调整语气，确保是表达自己而不是指责对方"
            ),
            "有一份按观察-感受-需要-请求四要素写的沟通稿，且请求是具体可执行的",
            "只写观察和感受两部分，不写需要和请求",
            List.of("非暴力沟通", "沟通准备", "艰难对话", "人际")
    );

    public static final TaskTemplate PRACTICE_EXPRESSION = new TaskTemplate(
            "INTERPER_003",
            TaskCategory.INTERPERSONAL,
            "练习沟通表达",
            "对着镜子或手机录音，练习你准备好的沟通内容，调整语气和表达方式",
            15,
            TaskDifficulty.EASY,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 拿出准备好的沟通稿",
                    "2. 对着镜子或打开手机录音，自然地说一遍（不要念稿，用自己的话）",
                    "3. 回听/回看，注意：语气是否太冲？语速是否太快？有没有指责的词？",
                    "4. 调整后再说一遍，对比两次的区别",
                    "5. 选一个你觉得最自然的版本，记住那种感觉"
            ),
            "录音练习至少2遍，并有1句话记录自己的改进点",
            "只对着镜子说一遍，不录音不回听",
            List.of("表达练习", "沟通", "录音", "人际")
    );

    public static final TaskTemplate SET_BOUNDARY = new TaskTemplate(
            "INTERPER_004",
            TaskCategory.INTERPERSONAL,
            "设定一个个人边界",
            "识别一个你一直在妥协但其实不舒服的场景，准备一句温和而坚定的边界表达",
            15,
            TaskDifficulty.MEDIUM,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 想一个你经常说'好'但其实心里不舒服的场景（如帮别人做事/参加不想去的活动）",
                    "2. 写下：如果不设边界，长期会怎样？（消耗/怨恨/影响自己的事）",
                    "3. 准备一句温和而坚定的拒绝/边界表达，模板：'感谢想到我，但我最近___，所以这次___'",
                    "4. 准备好如果对方追问或施压，你的第二句回应（如'我理解你需要___，但我确实___'）",
                    "5. 对着镜子说2遍，让自己习惯说出口"
            ),
            "有一个具体场景的边界表达（含拒绝话术和应对追问的第二句），并练习过2遍",
            "只想清楚场景和第一句拒绝话术，不准备第二句也不练习",
            List.of("边界", "拒绝", "自我照顾", "人际")
    );

    public static final TaskTemplate REACH_OUT = new TaskTemplate(
            "INTERPER_005",
            TaskCategory.INTERPERSONAL,
            "主动联系一个支持你的人",
            "给一个你信任的、能给你支持的人发一条消息或打个电话，分享你的近况或只是聊聊",
            15,
            TaskDifficulty.EASY,
            TimeOfDay.DAYTIME,
            List.of(
                    "1. 从通讯录里选一个你信任且最近没怎么联系的人（朋友/家人/同学）",
                    "2. 发一条消息：可以是'最近怎么样？'或分享一件你最近的小事",
                    "3. 如果对方回复了，聊5-10分钟",
                    "4. 如果方便，可以说一句最近的状态（不需要说细节，'最近有点累'就够了）",
                    "5. 结束后记录：联系后感觉怎么样？有没有轻松一点？"
            ),
            "主动给一个人发了消息并有互动，记录了联系后的感受",
            "只发一条消息（如'最近好吗'），不要求对方回复也不记录感受",
            List.of("社交连接", "主动联系", "社会支持", "人际")
    );

    public static List<TaskTemplate> all() {
        return List.of(
                CLARIFY_CONFLICT,
                PREPARE_CONVERSATION,
                PRACTICE_EXPRESSION,
                SET_BOUNDARY,
                REACH_OUT
        );
    }
}
