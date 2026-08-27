package com.example.demo.storage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.PlanTask;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.SupportPhase;
import com.example.demo.agent.contract.TaskStatus;
import com.example.demo.agent.contract.UserProfile;
import com.example.demo.agent.contract.SafetyLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportStateStoreTest {

    private static final String USER_ID = "u-storage-1";
    private static final LocalDate DAY_ONE = LocalDate.of(2026, 8, 26);

    @Test
    void upsertsDailyRecordsAndQueriesInclusiveDateRange() {
        InMemorySupportStateStore store = new InMemorySupportStateStore();
        store.saveUserProfile(profile());
        store.saveScreening(screening(DAY_ONE, 7));
        store.saveScreening(screening(DAY_ONE, 5));
        store.saveCheckIn(checkIn(DAY_ONE, "旧摘要"));
        store.saveCheckIn(checkIn(DAY_ONE, "最新摘要"));
        store.saveCheckIn(checkIn(DAY_ONE.plusDays(1), "第二天摘要"));

        assertEquals(1, store.findScreenings(USER_ID).size());
        assertEquals(5, store.findScreenings(USER_ID).getFirst().stressLevel());
        assertEquals("最新摘要", store.findCheckIn(USER_ID, DAY_ONE).orElseThrow().note());
        assertEquals(2, store.findCheckIns(USER_ID, DAY_ONE, DAY_ONE.plusDays(1)).size());
    }

    @Test
    void keepsPlanVersionsSeparateAndUpdatesTaskStatus() {
        InMemorySupportStateStore store = new InMemorySupportStateStore();
        store.saveUserProfile(profile());
        store.savePlanVersion(planVersion("v1", Instant.parse("2026-08-26T01:00:00Z"), "task-1"));
        store.savePlanVersion(planVersion("v2", Instant.parse("2026-08-27T01:00:00Z"), "task-1"));

        store.updateTaskStatus(USER_ID, "v1", DAY_ONE, "task-1", TaskStatus.DONE);

        assertEquals(List.of("v1", "v2"), store.findPlanVersions(USER_ID).stream()
                .map(PlanVersion::versionId)
                .toList());
        assertEquals(TaskStatus.DONE, store.findPlanVersion(USER_ID, "v1").orElseThrow()
                .planDays().getFirst().tasks().getFirst().status());
        assertEquals(TaskStatus.TODO, store.findPlanVersion(USER_ID, "v2").orElseThrow()
                .planDays().getFirst().tasks().getFirst().status());
    }

    @Test
    void restoresStateFromJsonAfterStoreRecreation() throws IOException {
        Path file = Path.of("target", "m4-support-state-test.json");
        Files.deleteIfExists(file);
        FileSupportStateStore first = new FileSupportStateStore(file);
        first.saveUserProfile(profile());
        first.saveScreening(screening(DAY_ONE, 8));
        first.savePlanVersion(planVersion("v1", Instant.parse("2026-08-26T01:00:00Z"), "task-1"));
        first.saveCheckIn(checkIn(DAY_ONE, "文件恢复摘要"));

        FileSupportStateStore restarted = new FileSupportStateStore(file);

        assertEquals("小孙", restarted.findUserProfile(USER_ID).orElseThrow().displayName());
        assertEquals(8, restarted.findScreenings(USER_ID).getFirst().stressLevel());
        assertEquals("文件恢复摘要", restarted.findCheckIn(USER_ID, DAY_ONE).orElseThrow().note());
        assertEquals(1, restarted.findPlanVersions(USER_ID).size());
        Files.deleteIfExists(file);
    }

    @Test
    void deletesAllUserDataAndRejectsInvalidQueries() {
        InMemorySupportStateStore store = new InMemorySupportStateStore();
        store.saveUserProfile(profile());

        assertThrows(IllegalArgumentException.class, () ->
                store.findCheckIns(USER_ID, DAY_ONE.plusDays(1), DAY_ONE));
        assertThrows(IllegalStateException.class, () ->
                store.saveCheckIn(checkInFor("unknown-user", DAY_ONE, "不应写入")));

        store.deleteUserData(USER_ID);

        assertFalse(store.findState(USER_ID).isPresent());
        assertTrue(store.findCheckIns(USER_ID, DAY_ONE, DAY_ONE).isEmpty());
    }

    private static UserProfile profile() {
        return new UserProfile(
                USER_ID, "小孙", List.of("考试压力"), DAY_ONE, SupportPhase.PLAN_ACTIVE, SafetyLevel.NORMAL
        );
    }

    private static ScreeningResult screening(LocalDate date, int stressLevel) {
        return new ScreeningResult(
                USER_ID, date, stressLevel, true, false, List.of("考试"), "考试压力", SupportPhase.PLANNING
        );
    }

    private static CheckInRecord checkIn(LocalDate date, String note) {
        return checkInFor(USER_ID, date, note);
    }

    private static CheckInRecord checkInFor(String userId, LocalDate date, String note) {
        return new CheckInRecord(
                userId, date, 5, 7.0, 6, 0.5,
                List.of("task-1"), List.of(), note, SupportPhase.CHECK_IN
        );
    }

    private static PlanVersion planVersion(String versionId, Instant createdAt, String taskId) {
        PlanTask task = new PlanTask(
                taskId, "完成一项最小复习", "现实任务", "完成一页重点整理。", Duration.ofMinutes(20),
                TaskStatus.TODO, "形成一页清单", null
        );
        return new PlanVersion(
                USER_ID, versionId,
                List.of(new PlanDay(DAY_ONE, 1, List.of(task), "稳住节奏", false)),
                createdAt
        );
    }
}
