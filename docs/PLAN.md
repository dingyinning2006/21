# M4 连续状态存储计划

## 范围

负责人：孙迎勒

实现 M4-001、M4-002：用户档案、压力初筛、每日打卡、计划版本、任务状态的存储边界；提供内存实现和 JSON 文件实现，支持独立测试与重启恢复。

不在本次范围：真实 MongoDB Repository、微信入口、LLM 编排、RAG、阶段报告算法。

## 文件计划

| 文件 | 功能 | 检测点 |
| --- | --- | --- |
| `src/main/java/com/example/demo/storage/PlanVersion.java` | 计划版本标识、计划日集合、版本创建时间 | 非空校验；日期和 dayIndex 去重 |
| `src/main/java/com/example/demo/storage/StoredSupportState.java` | 用户最小连续状态快照 | 用户 ID 一致性；集合不可变；跨用户数据拒绝 |
| `src/main/java/com/example/demo/storage/SupportStorageException.java` | 持久化失败异常 | 文件不可写/不可读时明确抛出 |
| `src/main/java/com/example/demo/storage/SupportStateStore.java` | M1/M3/M6 依赖的存储接口 | 不暴露数据库实现细节 |
| `src/main/java/com/example/demo/storage/InMemorySupportStateStore.java` | 内存状态实现 | 多日查询、幂等 upsert、任务状态更新、删除 |
| `src/main/java/com/example/demo/storage/FileSupportStateStore.java` | JSON 文件实现 | 重启恢复；临时文件原子替换；失败不静默 |
| `src/test/java/com/example/demo/storage/SupportStateStoreTest.java` | M4 验收测试 | 领域数据流、幂等、隔离、重启恢复 |
| `src/main/java/com/example/demo/storage/SupportStorageConfig.java` | 为 Bot 注册文件存储 Bean | 启动时使用 `support.storage-file` |
| `src/main/java/com/example/demo/bot/handler/WechatSupportCheckInService.java` | 微信文字打卡适配 | 命令识别、字段校验、保存和近 7 天查询 |
| `src/test/java/com/example/demo/bot/handler/WechatSupportCheckInServiceTest.java` | 微信入口测试 | 打卡、缺字段、普通消息透传 |
| `docs/M4-连续状态存储-完成报告.md` | 完成情况和风险 | 逐条对应 M4 验收标准 |

## 测试方案

- 单元：状态模型边界、日期区间、用户隔离、重复记录覆盖、任务状态更新。
- 集成：`FileSupportStateStore` 写入 JSON 后重新实例化，确认状态恢复。
- 连通性：执行全量 Maven 测试，确认既有领域契约测试与 M4 测试共同通过。

## 完成条件

- [x] M4 存储接口不依赖具体数据库。
- [x] 内存实现可供其他模块独立测试。
- [x] 同一用户同一天打卡不会产生重复记录。
- [x] 多个计划版本可区分并可更新任务状态。
- [x] 文件实现支持重启恢复，写失败抛出可识别异常。
- [x] 文档说明敏感数据最小化、删除和保留策略。
