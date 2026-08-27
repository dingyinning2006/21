package com.example.demo.storage;

import com.example.demo.agent.contract.CheckInRecord;
import com.example.demo.agent.contract.ScreeningResult;
import com.example.demo.agent.contract.TaskStatus;
import com.example.demo.agent.contract.UserProfile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** M1、M3、M6 使用的存储边界；实现可以是内存、文件或数据库。 */
public interface SupportStateStore {

    void saveUserProfile(UserProfile profile);

    Optional<UserProfile> findUserProfile(String userId);

    void saveScreening(ScreeningResult screening);

    List<ScreeningResult> findScreenings(String userId);

    void savePlanVersion(PlanVersion planVersion);

    List<PlanVersion> findPlanVersions(String userId);

    Optional<PlanVersion> findPlanVersion(String userId, String versionId);

    void saveCheckIn(CheckInRecord checkIn);

    Optional<CheckInRecord> findCheckIn(String userId, LocalDate date);

    List<CheckInRecord> findCheckIns(String userId, LocalDate from, LocalDate to);

    void updateTaskStatus(String userId, String versionId, LocalDate date, String taskId, TaskStatus status);

    Optional<StoredSupportState> findState(String userId);

    void deleteUserData(String userId);
}
