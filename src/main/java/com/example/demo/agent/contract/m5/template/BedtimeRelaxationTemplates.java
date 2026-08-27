package com.example.demo.agent.contract.m5.template;

import com.example.demo.agent.contract.m5.model.TaskCategory;
import com.example.demo.agent.contract.m5.model.TaskDifficulty;
import com.example.demo.agent.contract.m5.model.TaskTemplate;
import com.example.demo.agent.contract.m5.model.TimeOfDay;

import java.util.List;

/**
 * 睡前低唤醒任务模板。
 *
 * <p>对应压力源：睡眠紊乱。
 * 所有任务必须是低唤醒、放松性质，禁止包含需要动脑或令人兴奋的内容。
 * 建议在睡前30-60分钟执行。
 */
public final class BedtimeRelaxationTemplates {

    private BedtimeRelaxationTemplates() {}

    public static final TaskTemplate BREATHING_478 = new TaskTemplate(
            "BEDTIME_001",
            TaskCategory.BEDTIME_RELAXATION,
            "4-7-8呼吸放松",
            "通过4秒吸气-7秒屏息-8秒呼气的呼吸节奏，激活副交感神经，帮助身体进入放松状态",
            10,
            TaskDifficulty.EASY,
            TimeOfDay.BEDTIME,
            List.of(
                    "1. 躺到床上或坐在床边，保持舒适的姿势，闭上眼睛",
                    "2. 用鼻子轻轻吸气，心里默数4秒（1-2-3-4）",
                    "3. 屏住呼吸，默数7秒（1-2-3-4-5-6-7）",
                    "4. 用嘴巴慢慢呼气，默数8秒（1-2-3-4-5-6-7-8），呼气时发出轻轻的'呼'声",
                    "5. 这是一个循环，重复4个循环",
                    "6. 做完后保持自然呼吸，感受身体的放松感"
            ),
            "完成4个4-7-8呼吸循环，做完后感受身体放松",
            "只做2个循环，不严格计时，自然放慢呼吸即可",
            List.of("呼吸法", "4-7-8", "助眠", "放松", "睡前")
    );

    public static final TaskTemplate BODY_SCAN = new TaskTemplate(
            "BEDTIME_002",
            TaskCategory.BEDTIME_RELAXATION,
            "身体扫描放松",
            "从头到脚依次关注身体每个部位，有意识地放松肌肉，释放一天的紧张",
            15,
            TaskDifficulty.EASY,
            TimeOfDay.BEDTIME,
            List.of(
                    "1. 躺好，闭上眼睛，做3次深呼吸",
                    "2. 把注意力带到头顶，感受头皮的状态，有意识地放松额头和眉心",
                    "3. 慢慢下移：放松眼睛周围的肌肉→放松脸颊→放松下巴（松开咬紧的牙）",
                    "4. 继续下移：放松脖子和肩膀（肩膀往下沉，远离耳朵）→放松手臂→放松双手",
                    "5. 放松胸部和腹部（感受呼吸时腹部的起伏）→放松背部→放松腰部",
                    "6. 放松臀部→放松大腿→放松小腿→放松双脚和脚趾",
                    "7. 最后感受整个身体都放松了，像沉进床垫里一样",
                    "8. 如果某个部位特别紧张，在那里多停留一会儿，深呼吸3次"
            ),
            "从头到脚完成一遍身体扫描，每个部位都有意识地放松过",
            "只放松头部、肩膀和脚三个部位，每个部位停留30秒",
            List.of("身体扫描", "冥想", "肌肉放松", "助眠", "睡前")
    );

    public static final TaskTemplate PROGRESSIVE_MUSCLE = new TaskTemplate(
            "BEDTIME_003",
            TaskCategory.BEDTIME_RELAXATION,
            "渐进式肌肉放松",
            "通过'先紧张5秒再彻底放松'的方式，让身体体会紧张和放松的区别，最终进入深度放松",
            15,
            TaskDifficulty.EASY,
            TimeOfDay.BEDTIME,
            List.of(
                    "1. 躺好，闭上眼睛，做2次深呼吸",
                    "2. 双手：用力握紧拳头5秒，然后突然松开，感受放松感，停留10秒",
                    "3. 手臂：弯曲手臂用力绷紧肱二头肌5秒，然后松开，停留10秒",
                    "4. 肩膀：用力耸肩5秒，然后放下，感受肩膀沉下去，停留10秒",
                    "5. 脸部：用力皱眉+咬紧牙5秒，然后松开，舒展面部，停留10秒",
                    "6. 腹部：用力收紧腹部5秒，然后放松，感受腹部的起伏，停留10秒",
                    "7. 腿部：用力伸直腿+绷紧大腿5秒，然后松开，停留10秒",
                    "8. 脚部：用力勾脚趾5秒，然后松开，停留10秒",
                    "9. 最后，全身放松，感受那种沉甸甸的、温暖的放松感"
            ),
            "完成从头到脚的渐进式肌肉放松，每个部位都经历了紧张-放松的循环",
            "只做手、肩膀、脸三个部位的紧张-放松循环",
            List.of("渐进式肌肉放松", "PMR", "助眠", "放松", "睡前")
    );

    public static final TaskTemplate SCREEN_FREE_RITUAL = new TaskTemplate(
            "BEDTIME_004",
            TaskCategory.BEDTIME_RELAXATION,
            "睡前断网仪式",
            "在睡前30分钟把手机放到卧室外或伸手够不到的地方，用低刺激活动替代刷手机",
            10,
            TaskDifficulty.EASY,
            TimeOfDay.BEDTIME,
            List.of(
                    "1. 设定一个'手机 curfew'时间（如睡前30分钟）",
                    "2. 到时间后，把手机调到静音，放到卧室外或客厅（至少是伸手够不到的地方）",
                    "3. 做一件低刺激的事：看纸质书（不是电子书）/听轻音乐/写日记/简单拉伸",
                    "4. 调暗房间灯光，只留一盏暖光小夜灯",
                    "5. 如果忍不住想拿手机，告诉自己：'明天早上再看，现在是休息时间'",
                    "6. 躺到床上，做3次深呼吸，准备入睡"
            ),
            "睡前30分钟把手机放到够不到的地方，并用低刺激活动替代，调暗灯光",
            "只把手机放到床头柜抽屉里（不拿出去），调暗灯光，不做其他活动",
            List.of("断网", "睡眠卫生", "睡前仪式", "减少蓝光", "助眠")
    );

    public static final TaskTemplate GRATITUDE_JOURNAL = new TaskTemplate(
            "BEDTIME_005",
            TaskCategory.BEDTIME_RELAXATION,
            "睡前感恩日记",
            "在睡前写下3件今天值得感恩或开心的小事，帮助大脑从'问题模式'切换到'满足模式'",
            10,
            TaskDifficulty.EASY,
            TimeOfDay.BEDTIME,
            List.of(
                    "1. 拿出笔记本或手机备忘录（如果用手机，写完就放远）",
                    "2. 回想今天发生的事，写下3件值得感恩或让你开心的小事",
                    "3. 事情可以很小：'今天的饭很好吃'/'朋友回了我消息'/'今天阳光不错'",
                    "4. 对每件事，写一句话：为什么这件事让你觉得好？",
                    "5. 写完后，闭上眼睛，在心里再默念一遍这3件事",
                    "6. 带着这种满足感准备入睡"
            ),
            "写下3件今天值得感恩的小事，每件有一句话说明原因，并在心里默念过",
            "只在脑子里想3件好事，不写下来",
            List.of("感恩日记", "积极心理学", "睡前", "放松", "助眠")
    );

    public static final TaskTemplate GENTLE_STRETCH = new TaskTemplate(
            "BEDTIME_006",
            TaskCategory.BEDTIME_RELAXATION,
            "睡前温和拉伸",
            "做5个简单的床上拉伸动作，释放颈部、肩膀和背部的紧张，为睡眠做准备",
            10,
            TaskDifficulty.EASY,
            TimeOfDay.BEDTIME,
            List.of(
                    "1. 颈部拉伸：坐直，头慢慢倒向左侧，右手轻放头部右侧，保持15秒；换边，各做2次",
                    "2. 肩部绕环：双肩向前绕5圈，向后绕5圈，然后用力耸肩再放下，重复3次",
                    "3. 猫牛式：四肢跪姿（或坐着模仿），吸气时塌腰抬头，呼气时弓背低头，重复5次",
                    "4. 坐姿前屈：坐在床上，双腿伸直，慢慢向前弯腰，手去够脚尖（够不到没关系），保持20秒",
                    "5. 仰卧扭转：躺平，双膝弯曲倒向一侧，头转向另一侧，保持20秒；换边",
                    "6. 做完后躺平，感受身体的舒展和放松"
            ),
            "完成5个睡前拉伸动作，每个动作都保持了足够时间",
            "只做颈部拉伸和肩部绕环两个动作",
            List.of("拉伸", "床上运动", "释放紧张", "睡前", "助眠")
    );

    public static List<TaskTemplate> all() {
        return List.of(
                BREATHING_478,
                BODY_SCAN,
                PROGRESSIVE_MUSCLE,
                SCREEN_FREE_RITUAL,
                GRATITUDE_JOURNAL,
                GENTLE_STRETCH
        );
    }
}
