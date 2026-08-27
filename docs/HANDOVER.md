# M4 对接计划

## 本次完成

- `SupportStateStore`：统一存储接口。
- `InMemorySupportStateStore`：无外部依赖的测试实现。
- `FileSupportStateStore`：JSON 文件持久化和重启恢复实现。
- `PlanVersion`、`StoredSupportState`：计划版本和最小状态快照模型。
- `SupportStorageException`：读写失败的明确错误边界。
- JUnit 测试：幂等、多日查询、任务状态、重启恢复、删除和异常。
- 原始任务分工文档：`docs/压力调适Agent-任务分工.md`。

## 微信 Claw Bot 入口

已接入 `WechatMessageHandler`。Bot 收到以下文字时进入 M4：

- `开始打卡`：创建用户最小档案并返回格式。
- `打卡 压力=6 睡眠=7 心情=6 完成率=0.5 备注=完成最小任务`：保存当天 `CheckInRecord`。
- `查看打卡` 或 `我的打卡`：读取近 7 天记录。

文件存储默认位置：`data/support-state.json`，可由 `support.storage-file` 覆盖。天气、RAG、Function Calling 等非上述命令继续走原有链路。

## 与下游模块接口

M1、M3、M6 只依赖 `SupportStateStore`：

- M1 保存初筛、读取计划版本、更新任务状态。
- M3 保存每日文字/语音打卡，按日期读取已有打卡。
- M6 读取指定时间范围内的打卡和计划版本，计算阶段指标。

调用方先保存 `UserProfile`，再保存同一用户的初筛、计划或打卡。未知用户写入会抛出 `IllegalStateException`，文件读写失败会抛出 `SupportStorageException`，不能当作成功处理。

## 数据策略

- 只保存编排和报告需要的结构化字段。
- 原始语音、音频文件、模型请求全文不进入 M4 存储。
- `CheckInRecord.note` 是可选摘要；上游应传入必要短摘要，不传原始转写全文。
- `deleteUserData(userId)` 提供人工删除入口；生产环境应由定时任务按保留周期调用。
- 本地 JSON 实现用于演示和恢复测试；生产环境接入数据库时复用接口，不改变上层契约。

## 后续对接顺序

1. M3 将文字/语音打卡统一转换为 `CheckInRecord` 后调用 `saveCheckIn`。
2. M1 用 `findPlanVersions` 和 `updateTaskStatus` 实现计划调整。
3. M6 用 `findCheckIns(userId, from, to)` 读取阶段数据。
4. 真实数据库实现完成后，复用同一组存储契约测试。
