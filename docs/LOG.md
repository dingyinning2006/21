# M4 测试日志

## 2026-08-26 基线检查

### 测试方案

执行 `mvnw.cmd -q test`，确认前置契约测试基线。

### 结果

失败：当前 Maven Wrapper 在 PowerShell 环境报 `Cannot index into a null array`，系统未安装 `mvn` 命令。该失败属于本地构建工具环境，不是业务测试断言失败。

### 处理

保留问题，继续完成代码和测试；完成后再次尝试可用 Maven 路径，并记录最终结果。

---

## 2026-08-27 直连网络复测

用户连续发送多条消息后，日志仍只有 `/ilink/bot/getupdates` 的 HTTP retry，没有 `收到消息`；DNS 可解析，但直连微信接口 443 超时。因此当前阻塞点是运行环境无法直连微信接口，需要恢复可用代理或网络出口后，Bot 才能收到并回复消息。

## 2026-08-26 M4 存储测试

### 测试文件

`src/test/java/com/example/demo/storage/SupportStateStoreTest.java`

### 测试数据

- 用户：`u-storage-1`
- 日期：2026-08-26、2026-08-27
- 计划版本：`v1`、`v2`
- 任务：`task-1`

### 覆盖内容

- 初筛同日重复写入覆盖旧记录。
- 打卡同日重复写入覆盖旧记录，多日按闭区间查询。
- 计划版本隔离，`v1` 任务状态更新不影响 `v2`。
- JSON 文件重新实例化后恢复用户、初筛、打卡和计划版本。
- 未知用户写入、反向日期范围被拒绝。
- 删除用户数据后快照为空。

### 结果

`SupportStateStoreTest` 4/4 通过；`SupportDomainContractTest` 10/10 通过；组合执行共 14/14 通过。使用缓存 Maven 3.9.16 直接执行，未加载 Spring、MongoDB 或微信长轮询。

前置回归：`KeywordRagServiceTest` 3/3、`QwenServiceTest` 1/1、`ToolExecutorTest` 3/3、`SupportDomainContractTest` 10/10、`SupportStateStoreTest` 4/4，共 21/21 通过。

微信入口回归：`WechatSupportCheckInServiceTest` 3/3 通过。覆盖打卡保存、缺字段不写入、近 7 天查询和普通消息透传。

全量 `mvn test` 未作为通过依据：`DemoApplicationTests` 加载 Spring 上下文后启动既有微信轮询，并尝试连接本地 MongoDB；本地未运行 MongoDB，进程被停止。该问题属于前置启动测试的外部依赖，不影响上述隔离测试结果。

---

## 2026-08-27 微信 Bot 无回复排查

### 根因

SDK 2.3.3 在 `heartbeatEnabled=true` 时，会通过 `ilink-scheduler` 自动执行 `pollAndDispatchMessages()`；应用的 `WechatLoginRunner` 同时手动调用 `getUpdates()`。两个轮询消费者共享同一个更新游标，导致微信消息可能被其中一条路径消费而未交给业务处理器。

### 修复

- 关闭 SDK 自动心跳轮询。
- 保留应用手动消息循环及网络异常重试。
- 新增 `WechatBotConfigTest`，断言 SDK 自动轮询保持关闭。

### 验证

针对性测试 8/8 通过：`WechatBotConfigTest`、`WechatSupportCheckInServiceTest`、`SupportStateStoreTest`。重启后日志确认 Bot 恢复登录且未再出现 `ilink-scheduler`；当前运行实例 PID 为 25000，等待微信消息验证收发链路。

---
