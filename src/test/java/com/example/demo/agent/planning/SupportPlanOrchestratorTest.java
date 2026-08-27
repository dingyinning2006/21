package com.example.demo.agent.planning;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.PlanTask;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.StageReport;
import com.example.demo.agent.contract.SupportPhase;
import com.example.demo.agent.contract.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportPlanOrchestratorTest {

    private final SupportPlanOrchestrator orchestrator = new SupportPlanOrchestrator(
            new SevenDayPlanGenerator(),
            new PlanAdjustmentService()
    );

    @Test
    void generatesSevenDaysWithFourKindsOfTasks() {
        List<PlanDay> plan = orchestrator.startPlan(screeningResult(), LocalDate.of(2026, 8, 27));

        assertEquals(7, plan.size());
        assertEquals(1, plan.get(0).dayIndex());
        assertEquals(7, plan.get(6).dayIndex());
        assertEquals(4, plan.get(0).tasks().size());
        assertTrue(plan.get(0).tasks().stream().anyMatch(task -> "调适任务".equals(task.category())));
        assertTrue(plan.get(0).tasks().stream().anyMatch(task -> "现实任务".equals(task.category())));
        assertTrue(plan.get(0).tasks().stream().anyMatch(task -> "睡前任务".equals(task.category())));
        assertTrue(plan.get(0).tasks().stream().anyMatch(task -> "每日反馈".equals(task.category())));
    }

    @Test
    void reducesNextDayRealityTaskAfterDifficultCheckIn() {
        orchestrator.startPlan(screeningResult(), LocalDate.of(2026, 8, 27));

        orchestrator.recordCheckIn(new CheckInRecord(
                "plan-user",
                LocalDate.of(2026, 8, 27),
                9,
                5,
                4,
                0,
                List.of(),
                List.of("day-1-reality"),
                "今天很难集中注意力",
                SupportPhase.CHECK_IN
        ));

        PlanDay nextDay = orchestrator.getPlan("plan-user").get(1);
        PlanTask realityTask = nextDay.tasks().stream()
                .filter(task -> "现实任务".equals(task.category()))
                .findFirst()
                .orElseThrow();

        assertTrue(nextDay.reducedLoad());
        assertEquals(TaskStatus.REDUCED, realityTask.status());
        assertTrue(realityTask.title().startsWith("最小一步："));
        assertTrue(realityTask.estimatedDuration().toMinutes() <= 10);
    }

    @Test
    void buildsStageReportFromMultipleCheckIns() {
        orchestrator.startPlan(screeningResult(), LocalDate.of(2026, 8, 27));
        orchestrator.recordCheckIn(checkIn(
                LocalDate.of(2026, 8, 27), 8, 5.5, 0.5
        ));
        orchestrator.recordCheckIn(checkIn(
                LocalDate.of(2026, 8, 28), 6, 6.5, 1
        ));

        StageReport report = orchestrator.buildStageReport("plan-user");

        assertEquals(-2, report.stressDelta());
        assertEquals(1, report.sleepDelta());
        assertEquals(0.75, report.completionRate());
        assertTrue(report.summary().contains("2 次打卡"));
        assertFalse(report.nextStepSuggestions().isEmpty());
    }

    private ScreeningResult screeningResult() {
        return new ScreeningResult(
                "plan-user",
                LocalDate.of(2026, 8, 26),
                8,
                true,
                true,
                List.of("求职压力"),
                "求职压力",
                SupportPhase.PLANNING
        );
    }

    private CheckInRecord checkIn(LocalDate date, int stress, double sleep, double completion) {
        return new CheckInRecord(
                "plan-user",
                date,
                stress,
                sleep,
                6,
                completion,
                List.of(),
                List.of(),
                "记录当天状态",
                SupportPhase.CHECK_IN
        );
    }
}
