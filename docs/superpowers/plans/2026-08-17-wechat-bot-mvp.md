# 微信机器人 MVP（固定回复版）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> 本项目特殊约定：**代码由用户自己编写**，助手提供逐步指引（在哪里写、写什么、为什么），每步完成后由助手检查。因此本计划以"指引"形式执行，不使用子代理。

**Goal:** 手机微信给 bot 发消息，bot 回复固定文案"我是你的机器人，请等待问答"。

**Architecture:** 在现有 Spring Boot 项目里新增 `BotRunner`（`CommandLineRunner`），启动后扫码登录微信 iLink bot，随后进入长轮询循环 `getUpdates()`，收到消息由 `onMessage` 监听器统一回复固定文案。

**Tech Stack:** Spring Boot 4.0.7 / Java 17，wechat-ilink-sdk 2.3.3，ZXing 3.5.3（二维码 PNG）。

## Global Constraints

- 所有代码写在分支 `xuyichen-fe`，禁止推 main
- 构建命令用系统 `mvn`（本机 `./mvnw` 下载 Maven 发行版失败）
- 固定回复文案：`我是你的机器人，请等待问答`
- `qr.png`（登录二维码）不入 git
- 不做自动化测试：SDK 全部是微信网络交互，采用手动验证（启动/扫码/发消息）
- 代码由用户编写，助手检查；每完成一个 Task 提交一次

## 已核实的 SDK 行为（写代码的依据）

- `executeLogin()`：发起登录并返回二维码的**可扫描文本**（WeChat liteapp URL，如 `https://liteapp.weixin.qq.com/q/...`），直接用它渲染 PNG；每次调用都会创建**新的**登录 Future
- `getQrcode()`：返回 32 字符的原始 token，**不可扫码**，不要拿它渲染二维码（已实测验证）
- `getLoginFuture().get(2, TimeUnit.MINUTES)`：阻塞等扫码确认；超时≈二维码过期
- `cancelLogin()`：取消当前登录轮询，之后可再次 `executeLogin()`
- `getUpdates()`：**长轮询**拉消息，内部是 `pollAndDispatchMessages()`——收到消息会自动触发 `onMessage` 监听器；SDK 没有后台轮询线程，必须自己写循环调它
- `sendText(userId, text)`：发文本；对方必须先给 bot 发过消息（SDK 从入站消息缓存 contextToken）
- 消息模型：`WeixinMessage.getFrom_user_id()` / `getItem_list()`；`MessageItem.getText_item()`；`TextItem.getText()`

---

### Task 1: 添加依赖与 .gitignore

**Files:**
- Modify: `pom.xml`（`<dependencies>` 内、`</dependencies>` 之前插入三个依赖）
- Modify: `.gitignore`（末尾追加 `qr.png`）

**Interfaces:**
- Produces: Maven 依赖 `io.github.lith0924:wechat-ilink-sdk:2.3.3`、`com.google.zxing:core:3.5.3`、`com.google.zxing:javase:3.5.3` 供 Task 2 使用

- [ ] **Step 1.1: 在 pom.xml 添加三个依赖**

找到 `<dependencies>` 块，在最后一个 `</dependency>` 之后、`</dependencies>` 之前插入：

```xml
        <dependency>
            <groupId>io.github.lith0924</groupId>
            <artifactId>wechat-ilink-sdk</artifactId>
            <version>2.3.3</version>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>core</artifactId>
            <version>3.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>javase</artifactId>
            <version>3.5.3</version>
        </dependency>
```

为什么：SDK 已发布到 Maven 中央仓库，引依赖而不是拷源码——拷源码维护困难且容易像之前那样覆盖 pom；ZXing 的 `core` 负责二维码编码，`javase` 负责把二维码写成 PNG 图片。

- [ ] **Step 1.2: .gitignore 末尾追加 qr.png**

```gitignore
### Bot ###
qr.png
```

为什么：每次登录生成的二维码 2 分钟就过期，纯属本地临时文件，不能进仓库。

- [ ] **Step 1.3: 验证编译**

运行：`mvn -q -DskipTests compile`
预期：退出码 0，无报错（首次会下载 SDK 与 ZXing 依赖）。

- [ ] **Step 1.4: 提交**

```bash
git add pom.xml .gitignore
git commit -m "feat: 添加微信 SDK 与 ZXing 依赖"
```

---

### Task 2: BotRunner 登录闭环（生成二维码 + 扫码登录）

**Files:**
- Create: `src/main/java/com/example/demo/bot/BotRunner.java`

**Interfaces:**
- Consumes: Task 1 的 SDK 与 ZXing 依赖
- Produces: `BotRunner` 组件（字段 `ILinkClient client`、方法 `loginWithQrRetry()`、`generateQrCode(String, Path)`），Task 3 在其中追加 `handleMessages()` 与消息循环

**验收：** `mvn spring-boot:run` 启动 → 项目根目录生成 `qr.png` → 扫码后控制台打印"登录成功，botId = ..."

- [ ] **Step 2.1: 创建类骨架 + 构建客户端**

新建 `src/main/java/com/example/demo/bot/BotRunner.java`：

```java
package com.example.demo.bot;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.listener.OnLoginListener;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 微信机器人启动器：扫码登录 → 收消息 → 回复。
 * 实现 CommandLineRunner：Spring Boot 启动完成后会自动执行 run()。
 */
@Component
public class BotRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BotRunner.class);

    /** 二维码约 2 分钟过期，最多重新生成 5 次 */
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    /** SDK 客户端。用实例字段：Task 3 的 onMessage 回调要引用它，而它又必须在 builder 里注册回调——用字段解开这个"先有鸡还是先有蛋" */
    private ILinkClient client;

    @Override
    public void run(String... args) throws Exception {
        // Builder 模式构建客户端；onLogin 注册登录结果监听器（Task 3 再加 onMessage）
        client = ILinkClient.builder()
                .onLogin(new OnLoginListener() {
                    @Override
                    public void onLoginSuccess(LoginContext context) {
                        log.info("登录成功，botId = {}", context.getBotId());
                    }

                    @Override
                    public void onLoginFailure(Throwable throwable) {
                        log.error("登录失败: {}", throwable.getMessage());
                    }
                })
                .build();

        LoginContext context = loginWithQrRetry();
        if (context == null) {
            log.error("多次尝试后仍未登录成功，请重启程序再试");
            return;
        }
        log.info("登录完成，botId = {}", context.getBotId());
        // Task 3 会在这里加消息循环
    }

    /** 登录：生成二维码 PNG；扫码确认前阻塞；过期就重新生成，最多 5 次 */
    private LoginContext loginWithQrRetry() throws Exception {
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String qrContent = client.executeLogin(); // 向微信服务器发起登录，返回二维码可扫描文本
            generateQrCode(qrContent, Paths.get("qr.png"));
            log.info("第 {} 次生成二维码：请打开项目根目录下的 qr.png，用手机微信扫码登录", attempt);
            try {
                // 最多等 2 分钟；超时 = 二维码过期，重新生成
                return client.getLoginFuture().get(2, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                log.warn("二维码已过期，重新生成...");
                client.cancelLogin();
            } catch (ExecutionException e) {
                log.warn("登录失败: {}，重新生成二维码...", e.getMessage());
                client.cancelLogin();
            }
        }
        return null;
    }

    /** 用 ZXing 把文本渲染成 400x400 的二维码 PNG */
    private void generateQrCode(String content, Path filePath) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, 400, 400, hints);
        MatrixToImageWriter.writeToPath(matrix, "PNG", filePath);
    }
}
```

为什么这样写：
- `@Component` + `CommandLineRunner`：Spring 启动完自动跑 `run()`，bot 逻辑天然挂在应用生命周期上；`bot` 包在 `com.example.demo` 下，能被自动扫描到
- 直接用 `executeLogin()` 的返回值渲染：已从 SDK 字节码核实并实测，它返回的是可扫描的 liteapp URL；`getQrcode()` 返回的只是 32 字符 token，不可扫码（早期版本误用 `getQrcode()` 导致扫码乱码）
- 重试循环：微信二维码约 2 分钟过期，`get(2, TimeUnit.MINUTES)` 超时后 `cancelLogin()` 停掉旧轮询再重新登录（已核实 `executeLogin()` 每次都会生成新 Future，重试是安全的）

- [ ] **Step 2.2: 编译验证**

运行：`mvn -q -DskipTests compile`
预期：退出码 0。

- [ ] **Step 2.3: 运行验证登录闭环**

运行：`mvn spring-boot:run`
预期：
1. 控制台打印"第 1 次生成二维码..."，项目根目录出现 `qr.png`
2. 打开 qr.png，用手机微信扫码并确认
3. 控制台打印"登录成功，botId = ..."和"登录完成"

注意：用哪个微信扫码，那个号就成为 bot（建议用专门的小号）。扫码后程序不会退出，`Ctrl+C` 停止。

- [ ] **Step 2.4: 提交**

```bash
git add src/main/java/com/example/demo/bot/BotRunner.java
git commit -m "feat: BotRunner 扫码登录闭环"
```

---

### Task 3: 收消息 + 固定回复

**Files:**
- Modify: `src/main/java/com/example/demo/bot/BotRunner.java`（追加 `onMessage` 监听器、`handleMessages()`、消息循环）

**Interfaces:**
- Consumes: Task 2 的 `BotRunner.client` 字段、`LoginContext` 登录流程
- Produces: 完整的 MVP：登录 → 长轮询 → 固定回复

**验收：** 手机给 bot 发文字 → 控制台打印消息内容 → 手机收到"我是你的机器人，请等待问答"

- [ ] **Step 3.1: builder 里追加 onMessage 监听器**

把 `run()` 里的 builder 链改为（新增 `.onMessage(...)` 一段，其余不动）：

```java
        client = ILinkClient.builder()
                .onLogin(new OnLoginListener() {
                    @Override
                    public void onLoginSuccess(LoginContext context) {
                        log.info("登录成功，botId = {}", context.getBotId());
                    }

                    @Override
                    public void onLoginFailure(Throwable throwable) {
                        log.error("登录失败: {}", throwable.getMessage());
                    }
                })
                .onMessage(new OnMessageListener() {
                    @Override
                    public void onMessages(List<WeixinMessage> messages) {
                        handleMessages(messages);
                    }
                })
                .build();
```

同时补两个 import：

```java
import com.github.wechat.ilink.sdk.core.listener.OnMessageListener;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import java.util.List;
```

为什么：`onMessages` 在 `getUpdates()` 拉到消息时被 SDK 自动回调（已从字节码核实 `getUpdates → pollAndDispatchMessages`），我们把每条消息交给 `handleMessages` 处理。

- [ ] **Step 3.2: 追加 handleMessages 方法**

```java
    /** 收到新消息的回调：打印 + 固定回复 */
    private void handleMessages(List<WeixinMessage> messages) {
        for (WeixinMessage msg : messages) {
            String fromUserId = msg.getFrom_user_id();
            List<MessageItem> items = msg.getItem_list();
            if (items == null) {
                continue;
            }
            for (MessageItem item : items) {
                TextItem textItem = item.getText_item();
                if (textItem == null) {
                    continue; // 图片/语音等非文字消息，MVP 阶段先跳过
                }
                log.info("收到消息 fromUserId={}, text={}", fromUserId, textItem.getText());
                try {
                    // 前提：对方必须先给我们发过消息，SDK 才有 contextToken 可回复
                    client.sendText(fromUserId, FIXED_REPLY);
                } catch (Exception e) {
                    log.error("回复失败: {}", e.getMessage());
                }
            }
        }
    }
```

补 import：

```java
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.TextItem;
```

并在类字段处加固定文案常量：

```java
    /** 收到消息后统一回复的固定文案（下一步接大模型时替换这里） */
    private static final String FIXED_REPLY = "我是你的机器人，请等待问答";
```

为什么：每条消息可能带多个 item（多图、混合内容），所以两层循环；非文字先跳过，为下一步识别图片留接口。

- [ ] **Step 3.3: 登录成功后追加消息循环**

把 `run()` 末尾的 `// Task 3 会在这里加消息循环` 替换为：

```java
        // 长轮询收消息：getUpdates() 阻塞等新消息，收到后自动触发 onMessage。
        // SDK 没有后台线程，这个循环必须自己写。
        while (true) {
            try {
                List<WeixinMessage> messages = client.getUpdates();
                if (messages.isEmpty()) {
                    Thread.sleep(500); // 空轮询稍等片刻，避免空转
                }
            } catch (Exception e) {
                log.warn("拉取消息失败: {}", e.getMessage());
                Thread.sleep(2000); // 出错后等 2 秒再继续，避免疯狂重试
            }
        }
```

为什么：这是 SDK 的机制——长轮询阻塞直到有消息或超时；异常不能中断循环（网络抖动很正常），sleep 防止打爆服务器。

- [ ] **Step 3.4: 编译验证**

运行：`mvn -q -DskipTests compile`
预期：退出码 0。

- [ ] **Step 3.5: 运行验收（MVP 完成标准）**

运行：`mvn spring-boot:run`
预期（三条全满足即完成）：
1. 扫码登录成功，打印 botId
2. 手机微信给 bot 发"你好"：控制台打印 `收到消息 fromUserId=xxx@im.wechat, text=你好`
3. 手机收到回复"我是你的机器人，请等待问答"

- [ ] **Step 3.6: 提交**

```bash
git add src/main/java/com/example/demo/bot/BotRunner.java
git commit -m "feat: 收消息并回复固定文案，MVP 完成"
```

---

## 常见问题预案

| 现象 | 原因与处理 |
|---|---|
| 扫码后一直不成功 | 二维码过期，程序会自动重新生成（看第 N 次提示） |
| 手机发消息收不到回复 | 检查控制台是否打印"收到消息"；没打印说明循环没跑起来或没登录成功 |
| 控制台打印了消息但回复失败 | 日志里有"回复失败"；确认是对方先主动发的消息（contextToken 前提） |
| 程序报连接超时 | 网络问题，循环会自动重试；多次失败检查网络/代理 |
