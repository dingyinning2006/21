package com.example.demo.storage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.PlanDay;
import com.example.demo.agent.contract.PlanTask;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.TaskStatus;
import com.example.demo.agent.contract.UserProfile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 默认测试实现；按用户保存快照，重复日期数据采用幂等 upsert。 */
public class InMemorySupportStateStore implements SupportStateStore {

    private final Map<String, StoredSupportState> states = new HashMap<>();
    private final Clock clock;

    public InMemorySupportStateStore() {
        this(Clock.systemUTC());
    }

    public InMemorySupportStateStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock 不能为空");
    }

    @Override
    public synchronized void saveUserProfile(UserProfile profile) {
        Objects.requireNonNull(profile, "profile 不能为空");
        StoredSupportState current = states.get(profile.userId());
        states.put(profile.userId(), new StoredSupportState(
                profile.userId(),
                profile,
                current == null ? List.of() : current.screenings(),
                current == null ? List.of() : current.planVersions(),
                current == null ? List.of() : current.checkIns(),
                now()
        ));
        afterMutation();
    }

    @Override
    public synchronized Optional<UserProfile> findUserProfile(String userId) {
        return findState(userId).map(StoredSupportState::userProfile);
    }

    @Override
    public synchronized void saveScreening(ScreeningResult screening) {
        Objects.requireNonNull(screening, "screening 不能为空");
        StoredSupportState current = requireState(screening.userId());
        List<ScreeningResult> screenings = new ArrayList<>(current.screenings().stream()
                .filter(item -> !item.recordedOn().equals(screening.recordedOn()))
                .toList());
        screenings.add(screening);
        screenings.sort(Comparator.comparing(ScreeningResult::recordedOn));
        replace(current, screenings, current.planVersions(), current.checkIns());
    }

    @Override
    public synchronized List<ScreeningResult> findScreenings(String userId) {
        return findState(userId)
                .map(StoredSupportState::screenings)
                .orElseGet(List::of);
    }

    @Override
    public synchronized void savePlanVersion(PlanVersion planVersion) {
        Objects.requireNonNull(planVersion, "planVersion 不能为空");
        StoredSupportState current = requireState(planVersion.userId());
        List<PlanVersion> versions = new ArrayList<>(current.planVersions().stream()
                .filter(item -> !item.versionId().equals(planVersion.versionId()))
                .toList());
        versions.add(planVersion);
        versions.sort(Comparator.comparing(PlanVersion::createdAt).thenComparing(PlanVersion::versionId));
        replace(current, current.screenings(), versions, current.checkIns());
    }

    @Override
    public synchronized List<PlanVersion> findPlanVersions(String userId) {
        return findState(userId)
                .map(StoredSupportState::planVersions)
                .orElseGet(List::of);
    }

    @Override
    public synchronized Optional<PlanVersion> findPlanVersion(String userId, String versionId) {
        requireNonBlank(versionId, "versionId");
        return findPlanVersions(userId).stream()
                .filter(version -> version.versionId().equals(versionId))
                .findFirst();
    }

    @Override
    public synchronized void saveCheckIn(CheckInRecord checkIn) {
        Objects.requireNonNull(checkIn, "checkIn 不能为空");
        StoredSupportState current = requireState(checkIn.userId());
        List<CheckInRecord> checkIns = new ArrayList<>(current.checkIns().stream()
                .filter(item -> !item.date().equals(checkIn.date()))
                .toList());
        checkIns.add(checkIn);
        checkIns.sort(Comparator.comparing(CheckInRecord::date));
        replace(current, current.screenings(), current.planVersions(), checkIns);
    }

    @Override
    public synchronized Optional<CheckInRecord> findCheckIn(String userId, LocalDate date) {
        Objects.requireNonNull(date, "date 不能为空");
        return findCheckIns(userId, date, date).stream().findFirst();
    }

    @Override
    public synchronized List<CheckInRecord> findCheckIns(String userId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from 不能为空");
        Objects.requireNonNull(to, "to 不能为空");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to 不能早于 from");
        }
        return findState(userId)
                .map(state -> state.checkIns().stream()
                        .filter(item -> !item.date().isBefore(from) && !item.date().isAfter(to))
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public synchronized void updateTaskStatus(
            String userId,
            String versionId,
            LocalDate date,
            String taskId,
            TaskStatus status
    ) {
        Objects.requireNonNull(date, "date 不能为空");
        Objects.requireNonNull(status, "status 不能为空");
        requireNonBlank(taskId, "taskId");
        StoredSupportState current = requireState(userId);
        PlanVersion version = findPlanVersion(userId, versionId)
                .orElseThrow(() -> new IllegalArgumentException("计划版本不存在: " + versionId));
        PlanDay oldDay = version.planDays().stream()
                .filter(day -> day.date().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("计划日期不存在: " + date));
        PlanTask oldTask = oldDay.tasks().stream()
                .filter(task -> task.taskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("计划任务不存在: " + taskId));
        PlanTask newTask = new PlanTask(
                oldTask.taskId(),
                oldTask.title(),
                oldTask.category(),
                oldTask.description(),
                oldTask.estimatedDuration(),
                status,
                oldTask.successCriteria(),
                oldTask.fallbackTaskId()
        );
        List<PlanTask> tasks = new ArrayList<>(oldDay.tasks());
        tasks.set(tasks.indexOf(oldTask), newTask);
        PlanDay newDay = new PlanDay(oldDay.date(), oldDay.dayIndex(), tasks, oldDay.focus(), oldDay.reducedLoad());
        List<PlanDay> days = new ArrayList<>(version.planDays());
        days.set(days.indexOf(oldDay), newDay);
        PlanVersion updatedVersion = new PlanVersion(version.userId(), version.versionId(), days, version.createdAt());
        List<PlanVersion> versions = new ArrayList<>(current.planVersions().stream()
                .filter(item -> !item.versionId().equals(versionId))
                .toList());
        versions.add(updatedVersion);
        versions.sort(Comparator.comparing(PlanVersion::createdAt).thenComparing(PlanVersion::versionId));
        replace(current, current.screenings(), versions, current.checkIns());
    }

    @Override
    public synchronized Optional<StoredSupportState> findState(String userId) {
        requireNonBlank(userId, "userId");
        return Optional.ofNullable(states.get(userId));
    }

    @Override
    public synchronized void deleteUserData(String userId) {
        requireNonBlank(userId, "userId");
        if (states.remove(userId) != null) {
            afterMutation();
        }
    }

    /** 文件实现使用该快照做完整替换；不暴露内部可变集合。 */
    public synchronized List<StoredSupportState> snapshot() {
        return states.values().stream()
                .sorted(Comparator.comparing(StoredSupportState::userId))
                .toList();
    }

    protected synchronized void restore(Collection<StoredSupportState> restoredStates) {
        Objects.requireNonNull(restoredStates, "restoredStates 不能为空");
        states.clear();
        for (StoredSupportState state : restoredStates) {
            if (states.put(state.userId(), state) != null) {
                throw new IllegalArgumentException("重复用户状态: " + state.userId());
            }
        }
    }

    protected void afterMutation() {
        // 文件实现覆写此钩子；内存实现无需额外操作。
    }

    private StoredSupportState requireState(String userId) {
        return findState(userId).orElseThrow(() -> new IllegalStateException("用户档案不存在: " + userId));
    }

    private void replace(
            StoredSupportState current,
            List<ScreeningResult> screenings,
            List<PlanVersion> planVersions,
            List<CheckInRecord> checkIns
    ) {
        states.put(current.userId(), new StoredSupportState(
                current.userId(), current.userProfile(), screenings, planVersions, checkIns, now()
        ));
        afterMutation();
    }

    private Instant now() {
        return clock.instant();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim();
    }
}
