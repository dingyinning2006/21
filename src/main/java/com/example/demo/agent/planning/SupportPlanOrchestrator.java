package com.example.demo.agent.planning;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.SafetyLevel;
import com.example.demo.agent.contract.StageReport;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M1-003 的首版编排服务。
 * 内存 Map 只是本地演示适配器，后续由 M4 替换为正式存储。
 */
@Service
public class SupportPlanOrchestrator {

    private final PlanGenerator planGenerator;
    private final PlanAdjustmentService planAdjustmentService;
    private final Map<String, ScreeningResult> screenings = new ConcurrentHashMap<>();
    private final Map<String, List<PlanDay>> plans = new ConcurrentHashMap<>();
    private final Map<String, List<CheckInRecord>> checkIns = new ConcurrentHashMap<>();

    public SupportPlanOrchestrator(
            PlanGenerator planGenerator,
            PlanAdjustmentService planAdjustmentService
    ) {
        this.planGenerator = planGenerator;
        this.planAdjustmentService = planAdjustmentService;
    }

    public synchronized List<PlanDay> startPlan(
            ScreeningResult screeningResult,
            LocalDate startDate
    ) {
        if (screeningResult == null) {
            throw new IllegalArgumentException("screeningResult 不能为空");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("startDate 不能为空");
        }

        screenings.put(screeningResult.userId(), screeningResult);
        return plans.computeIfAbsent(
                screeningResult.userId(),
                ignored -> List.copyOf(planGenerator.generate(screeningResult, startDate))
        );
    }

    public List<PlanDay> getPlan(String userId) {
        return plans.getOrDefault(userId, List.of());
    }

    public boolean hasPlan(String userId) {
        return !getPlan(userId).isEmpty();
    }

    public synchronized void recordCheckIn(CheckInRecord checkIn) {
        if (checkIn == null) {
            throw new IllegalArgumentException("checkIn 不能为空");
        }

        List<CheckInRecord> history = new ArrayList<>(
                checkIns.getOrDefault(checkIn.userId(), List.of())
        );
        history.removeIf(item -> item.date().equals(checkIn.date()));
        history.add(checkIn);
        history.sort(Comparator.comparing(CheckInRecord::date));
        checkIns.put(checkIn.userId(), List.copyOf(history));

        List<PlanDay> currentPlan = plans.get(checkIn.userId());
        if (currentPlan == null) {
            return;
        }

        List<PlanDay> adjustedPlan = new ArrayList<>(currentPlan);
        for (int index = 0; index < adjustedPlan.size(); index++) {
            PlanDay planDay = adjustedPlan.get(index);
            if (planDay.date().equals(checkIn.date().plusDays(1))) {
                adjustedPlan.set(index, planAdjustmentService.adjustNextDay(planDay, checkIn));
                break;
            }
        }
        plans.put(checkIn.userId(), List.copyOf(adjustedPlan));
    }

    public StageReport buildStageReport(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        List<CheckInRecord> history = checkIns.getOrDefault(userId, List.of());
        if (history.isEmpty()) {
            throw new IllegalStateException("还没有可用于生成报告的打卡记录");
        }

        CheckInRecord first = history.get(0);
        CheckInRecord latest = history.get(history.size() - 1);
        double averageCompletion = history.stream()
                .mapToDouble(CheckInRecord::completionRate)
                .average()
                .orElse(0);

        double stressDelta = latest.stressLevel() - first.stressLevel();
        double sleepDelta = latest.sleepHours() - first.sleepHours();
        List<String> changes = new ArrayList<>();
        changes.add(stressDelta < 0 ? "压力较初始记录下降" : stressDelta > 0 ? "压力较初始记录上升" : "压力暂未明显变化");
        changes.add(sleepDelta > 0 ? "睡眠时长有所增加" : sleepDelta < 0 ? "睡眠时长有所减少" : "睡眠时长暂未明显变化");
        changes.add("已完成 " + history.size() + " 次打卡");

        List<String> suggestions = new ArrayList<>();
        if (averageCompletion < 0.5) {
            suggestions.add("继续把现实任务缩小为 10 分钟内的最小动作");
        } else {
            suggestions.add("继续保持每日一个现实任务、一个调适任务和固定睡前活动");
        }
        if (sleepDelta < 0 || latest.sleepHours() < 6) {
            suggestions.add("优先稳定起床和入睡时间；若持续影响生活，考虑联系专业支持");
        }

        String summary = "这段时间共完成 " + history.size()
                + " 次打卡，平均任务完成率为 "
                + String.format("%.0f%%", averageCompletion * 100)
                + "。报告用于观察变化，不构成医学诊断。";

        return new StageReport(
                userId,
                first.date(),
                latest.date(),
                SafetyLevel.NORMAL,
                stressDelta,
                sleepDelta,
                averageCompletion,
                changes,
                suggestions,
                summary
        );
    }
}
