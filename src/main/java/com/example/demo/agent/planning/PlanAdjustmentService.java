package com.example.demo.agent.planning;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.PlanTask;
import com.example.demo.agent.contract.TaskStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** 根据当天反馈调整下一天计划，避免机械追加任务。 */
@Service
public class PlanAdjustmentService {

    public PlanDay adjustNextDay(PlanDay nextDay, CheckInRecord checkIn) {
        if (nextDay == null || checkIn == null) {
            throw new IllegalArgumentException("nextDay 和 checkIn 不能为空");
        }

        if (!shouldReduceLoad(checkIn)) {
            return nextDay;
        }

        List<PlanTask> adjustedTasks = nextDay.tasks().stream()
                .map(this::reduceRealityTask)
                .toList();

        return new PlanDay(
                nextDay.date(),
                nextDay.dayIndex(),
                adjustedTasks,
                nextDay.focus() + "（减负版）",
                true
        );
    }

    private boolean shouldReduceLoad(CheckInRecord checkIn) {
        return checkIn.completionRate() < 0.5
                || checkIn.stressLevel() >= 8
                || checkIn.sleepHours() < 6;
    }

    private PlanTask reduceRealityTask(PlanTask task) {
        if (!"现实任务".equals(task.category())) {
            return task;
        }

        return new PlanTask(
                task.taskId(),
                "最小一步：" + task.title(),
                task.category(),
                "今天只做原任务中最容易开始的一小步，完成后即可停止。",
                Duration.ofMinutes(Math.min(10, task.estimatedDuration().toMinutes())),
                TaskStatus.REDUCED,
                "完成一个最小动作即可，不要求完成原任务的全部内容",
                task.fallbackTaskId()
        );
    }
}
