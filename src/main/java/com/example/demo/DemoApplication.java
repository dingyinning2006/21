package com.example.demo;

import com.example.demo.llm.LlmService;
import com.example.demo.util.ConfigUtil;
import com.example.demo.wechat.WeChatClient;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DemoApplication {

	private static final ExecutorService executor = Executors.newFixedThreadPool(4);

	public static void main(String[] args) throws Exception {
		// 从配置文件读取，不再硬编码写死密钥
		String apiKey = ConfigUtil.get("llm.api-key");
		String baseUrl = ConfigUtil.get("llm.base-url");
		String modelEpId = ConfigUtil.get("llm.model");

		WeChatClient client = new WeChatClient();
		LlmService llmService = new LlmService(apiKey, baseUrl, modelEpId);

		client.login();
		System.out.println("========================================");
		System.out.println("  LLM 机器人已启动，等待 clawbot 消息...");
		System.out.println("========================================");

		while (true) {
			try {
				List<WeixinMessage> messages = client.getUpdates();

				for (WeixinMessage msg : messages) {
					String fromUserId = msg.getFrom_user_id();
					if (msg.getItem_list() == null) continue;

					for (MessageItem item : msg.getItem_list()) {
						// 文字消息处理
						if (item.getText_item() != null) {
							String userText = item.getText_item().getText();
							System.out.println("[收到] " + fromUserId + ": " + userText);

							executor.submit(() -> {
								System.out.println("[收到用户消息] " + fromUserId + " : " + userText);
								try {
									String reply = llmService.chat(userText);
									System.out.println("[✅ LLM拿到回复]：" + reply);
									client.sendText(fromUserId, reply);
									System.out.println("[✅ 微信消息发送完成]");
								} catch (Exception e) {
									System.err.println("[❌处理消息发生异常]");
									e.printStackTrace();
									try {
										client.sendText(fromUserId, "服务出错");
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							});
						}
						// 新增图片消息分支（你之前要做的识图功能）
						if (item.getImage_item() != null) {
							String imgUrl = item.getImage_item().getUrl();
							System.out.println("[收到图片消息] 图片链接：" + imgUrl);
							executor.submit(() -> {
								try {
									String reply = llmService.chat("详细描述这张图片", imgUrl);
									System.out.println("[✅ AI识图回复]：" + reply);
									client.sendText(fromUserId, reply);
								} catch (Exception e) {
									e.printStackTrace();
								}
							});
						}
					}
				}
			} catch (Exception e) {
				System.err.println("[拉取异常] " + e.getMessage());
			}

			Thread.sleep(2000);
		}
	}
}