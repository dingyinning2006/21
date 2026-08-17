# 微信机器人 MVP 设计（固定回复版）

- 日期：2026-08-17
- 状态：已确认
- 分支：xuyichen-fe

## 背景与目标

夏令营项目：做一个微信智能 Agent，用户通过微信发指令，电脑端自动执行任务。整体技术路线为：微信 SDK 连通 → 固定回复 → 接入大模型 → 多模态（文字/图片/语音）→ 意图识别。

本设计只覆盖**第一个里程碑：最小闭环**——扫码登录、收到消息、回复固定文案。大模型与多模态属于后续迭代，不在本设计范围内。

## 协作方式

用户自己写代码。助手提供"在哪里写、写什么、为什么这么写"的逐步指引，并在每步完成后帮用户检查代码。

## 范围

**In scope**
- pom.xml 添加 SDK 与 ZXing 依赖
- `BotRunner` 组件：登录、收消息、固定回复
- ZXing 生成登录二维码 PNG

**Out of scope（后续迭代）**
- 接入大模型智能回复
- 图片回复与图片内容识别
- 语音回复
- 意图识别
- 自动化测试（MVP 阶段依赖真实微信网络交互，手动验证即可）

## 技术方案

### 依赖（pom.xml 新增）

| 依赖 | 版本 | 用途 |
|---|---|---|
| io.github.lith0924:wechat-ilink-sdk | 2.3.3 | 微信 iLink 客户端（登录/收发消息/媒体） |
| com.google.zxing:core | 3.5.3 | 二维码生成 |
| com.google.zxing:javase | 3.5.3 | 二维码写出为 PNG |

### 新增文件

- `.gitignore` 增加 `qr.png`（登录二维码会过期，不应入库）
- `src/main/java/com/example/demo/bot/BotRunner.java`，实现 `CommandLineRunner`（Spring 启动完成后自动执行），单类包含：

1. **登录**：`ILinkClient.builder().onMessage(...).build()` → `executeLogin()` 获取二维码文本 → ZXing 渲染为项目根目录下的 `qr.png` → 提示用户打开扫码
2. **等待登录**：`getLoginFuture().get()` 阻塞至手机确认，成功后打印 `botId`
3. **收消息**：`onMessage` 监听器遍历 `WeixinMessage`，提取 `from_user_id` 与文字内容，打印到控制台
4. **固定回复**：`sendText(userId, "我是你的机器人，请等待问答")`

### 运行流程

```
mvn spring-boot:run
  → Spring 启动 → BotRunner.run()
  → 生成 qr.png，用户打开扫码
  → 登录成功，打印 botId
  → 手机发消息 → 控制台打印消息 → 手机收到固定回复
```

### 架构取舍

- 采用 Spring `CommandLineRunner` 组件方式（而非独立 main），便于后续接大模型时注入 Bean
- MVP 阶段逻辑集中于单类，接大模型时再拆分 `LLMService` 等组件（YAGNI）

## 已知坑位（写入代码注释）

1. 对方必须**先给 bot 发消息**，bot 才能回复——SDK 依赖 `getUpdates()` 拉到的 `contextToken` 建立会话上下文
2. 二维码会过期（登录轮询返回 EXPIRED），需重新生成
3. 登录态失效后需重启程序重新扫码，SDK 不支持自动重登

## 验证标准（全部满足即 MVP 完成）

1. `mvn spring-boot:run` 启动成功，项目根目录生成 `qr.png`
2. 手机扫码并确认后，控制台打印登录成功与 botId
3. 手机给 bot 发文字消息：控制台打印消息内容，手机收到"我是你的机器人，请等待问答"
4. API Key 与账号凭证不进入 git 仓库（本里程碑虽无 LLM Key，登录凭证由扫码产生，不入库）

## 后续迭代路线

1. 接入大模型（百炼/硅基流动免费模型，OpenAI 兼容接口），固定回复替换为智能回复
2. 图片消息：下载图片 → 视觉模型识别 → 文字回复；文生图 → `sendImage` 回复
3. 语音：TTS → `sendVoice` 回复
4. 意图识别：单一功能模型判断回复类型（文字/图片/语音）
