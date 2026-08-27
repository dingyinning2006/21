package com.example.demo.agent.planning;

import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.PlanTask;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.TaskStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 首版可演示的 7 天游程生成器。
 * 任务模板保持简单，后续可由 M5 替换为更丰富的场景模板。
 */
@Service
public class SevenDayPlanGenerator implements PlanGenerator {

    @Override
    public List<PlanDay> generate(ScreeningResult screeningResult, LocalDate startDate) {
        if (screeningResult == null) {
            throw new IllegalArgumentException("screeningResult 不能为空");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("startDate 不能为空");
        }

        List<PlanDay> plan = new ArrayList<>();
        for (int dayIndex = 1; dayIndex <= 7; dayIndex++) {
            LocalDate date = startDate.plusDays(dayIndex - 1L);
            plan.add(new PlanDay(
                    date,
                    dayIndex,
                    List.of(
                            buildRegulationTask(dayIndex),
                            buildRealityTask(screeningResult, dayIndex),
                            buildSleepTask(dayIndex),
                            buildCheckInTask(dayIndex)
                    ),
                    dayFocus(dayIndex),
                    false
            ));
        }
        return List.copyOf(plan);
    }

    private PlanTask buildRegulationTask(int dayIndex) {
        return new PlanTask(
                "day-" + dayIndex + "-regulation",
                "3 分钟呼吸放松",
                "调适任务",
                "找一个相对安静的位置，缓慢吸气和呼气各 3 秒，持续 3 分钟。",
                Duration.ofMinutes(3),
                TaskStatus.TODO,
                "完成 3 分钟练习，并记录练习前后的紧张程度",
                null
        );
    }

    private PlanTask buildRealityTask(ScreeningResult result, int dayIndex) {
        String taskTitle;
        String description;
        String criteria;
        String category;

        if (result.mainStressor().contains("求职")) {
            category = "现实任务";
            taskTitle = switch (dayIndex) {
                case 1 -> "整理 3 个高频面试问题";
                case 2 -> "完成一段 1 分钟自我介绍";
                case 3 -> "复盘一次面试经历";
                case 4 -> "准备一个项目案例";
                case 5 -> "练习一次模拟回答";
                case 6 -> "检查简历中的一处表述";
                default -> "整理面试前物品和路线";
            };
            description = "把求职准备拆成一个今天可以完成的小步骤，不追求一次做完全部准备。";
            criteria = "完成“" + taskTitle + "”并留下一个可查看的结果";
        } else if (result.mainStressor().contains("考试")) {
            category = "现实任务";
            taskTitle = "完成一组重点复习";
            description = "选择一个最重要的小知识点，专注复习 20 分钟。";
            criteria = "完成 20 分钟复习并写下 3 条要点";
        } else {
            category = "现实任务";
            taskTitle = dayIndex == 1 ? "列出一个最小行动" : "完成一个当天最重要的小步骤";
            description = "把当前压力对应的事情缩小为一个明确、可在今天完成的动作。";
            criteria = "完成一个具体动作，而不是只停留在计划层面";
        }

        return new PlanTask(
                "day-" + dayIndex + "-reality",
                taskTitle,
                category,
                description,
                Duration.ofMinutes(dayIndex == 1 ? 15 : 20),
                TaskStatus.TODO,
                criteria,
                null
        );
    }

    private PlanTask buildSleepTask(int dayIndex) {
        return new PlanTask(
                "day-" + dayIndex + "-sleep",
                "睡前低唤醒活动",
                "睡前任务",
                "睡前 30 分钟放下高刺激内容，进行简单拉伸、听舒缓音乐或阅读纸质内容。",
                Duration.ofMinutes(10),
                TaskStatus.TODO,
                "完成至少 10 分钟低唤醒活动，并尽量固定上床时间",
                null
        );
    }

    private PlanTask buildCheckInTask(int dayIndex) {
        return new PlanTask(
                "day-" + dayIndex + "-check-in",
                "完成今日状态打卡",
                "每日反馈",
                "记录今天的压力、睡眠、心情和任务完成情况。",
                Duration.ofMinutes(5),
                TaskStatus.TODO,
                "提交一条文字或语音打卡",
                null
        );
    }

    private String dayFocus(int dayIndex) {
        return switch (dayIndex) {
            case 1 -> "先稳住节奏，完成一个最小行动";
            case 2, 3 -> "逐步恢复行动感";
            case 4, 5 -> "练习并巩固";
            default -> "复盘并准备下一步";
        };
    }
}
