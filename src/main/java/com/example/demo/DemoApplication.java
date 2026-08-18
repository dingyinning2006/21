package com.example.demo;

import com.example.demo.llm.LlmService;
import com.example.demo.speech.SpeechSynthesisService;
import com.example.demo.util.ConfigUtil;
import com.example.demo.wechat.WeChatClient;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.example.demo.intent.IntentRecognizer;
import com.example.demo.weather.WeatherClient;
import com.example.demo.weather.WeatherResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.demo.speech.SpeechRecognitionService;
import com.example.demo.speech.SpeechSynthesisService;

public class DemoApplication {

	private static final ExecutorService executor = Executors.newFixedThreadPool(4);

	public static void main(String[] args) throws Exception {
		// 从配置文件读取，不再硬编码写死密钥
		String apiKey = ConfigUtil.get("llm.api-key");
		String baseUrl = ConfigUtil.get("llm.base-url");
		String modelEpId = ConfigUtil.get("llm.model");

		WeChatClient client = new WeChatClient();
		IntentRecognizer intentRecognizer = new IntentRecognizer();   // 新增
		WeatherClient weatherClient = new WeatherClient();   // 新增
		LlmService llmService = new LlmService(apiKey, baseUrl, modelEpId);
// 百度语音识别


		SpeechRecognitionService asrService = new SpeechRecognitionService();
		SpeechSynthesisService ttsService = new SpeechSynthesisService();  // 新增
		client.login();
		// 新增：消费掉登录前的历史消息，避免第一条新消息被忽略
		client.getUpdates();

		System.out.println("========================================");
		System.out.println("  LLM 机器人已启动，等待 clawbot 消息...");
		System.out.println("========================================");

		System.out.println("========================================");
		System.out.println("  LLM 机器人已启动，等待 clawbot 消息...");
		System.out.println("========================================");
		final boolean[] replyByVoice = {false};
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
									String reply;
// 用意图识别器判断
									IntentRecognizer.IntentType intent = intentRecognizer.recognize(userText);
									switch (intent) {
										case WEATHER -> {
											reply = getWeatherReply(userText, weatherClient, intentRecognizer);
											System.out.println("[✅ 天气查询回复]：" + reply);
										}
										case HELP -> {
											reply = getHelpReply();
											System.out.println("[✅ 帮助回复]");
										}case VOICE_REPLY -> {
											replyByVoice[0] = true;
											reply = "✅ 已切换为语音回复模式（当前微信暂不支持发送语音，功能开发中）";
											System.out.println("[✅ 切换语音回复模式]");
										}
										case TEXT_REPLY -> {
											replyByVoice[0] = false;
											reply = "✅ 已切换为文字回复模式";
											System.out.println("[✅ 切换文字回复模式]");
										}
										default -> {
											reply = llmService.chat(userText);
											System.out.println("[✅ LLM拿到回复]：" + reply);
										}
									}
									// ====================================
									if (replyByVoice[0]) {
										// 语音回复模式：生成 mp3 并发文件
										byte[] mp3Bytes = ttsService.synthesize(reply);
										if (mp3Bytes != null) {
											client.sendFile(fromUserId, mp3Bytes, "语音回复.mp3", "");
											System.out.println("[✅ 语音回复发送完成]");
										} else {
											client.sendText(fromUserId, "【语音生成失败】\n" + reply);
										}
									} else {
										client.sendText(fromUserId, reply);
									}
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
						// 语音消息处理
						if (item.getVoice_item() != null) {
							System.out.println("[收到语音消息] from=" + fromUserId
									+ ", duration=" + item.getVoice_item().getPlaytime() + "ms"
									+ ", encodeType=" + item.getVoice_item().getEncode_type());
							try {
								byte[] voiceBytes = client.downloadVoice(item);

// 打印前10字节的十六进制，判断格式
								StringBuilder hex = new StringBuilder();
								for (int i = 0; i < Math.min(10, voiceBytes.length); i++) {
									hex.append(String.format("%02X ", voiceBytes[i]));
								}
								System.out.println("[语音文件头] " + hex.toString());
// 也可以转成字符串看
								System.out.println("[语音文件头(文本)] " + new String(voiceBytes, 0, Math.min(20, voiceBytes.length)));
								System.out.println("[语音下载成功] 大小=" + voiceBytes.length + " bytes");

								// ASR 识别，格式先用 amr（微信语音常见格式）
								String recognizedText = asrService.recognize(voiceBytes);

								if (recognizedText != null && !recognizedText.isBlank()) {
									System.out.println("[✅ 语音识别结果] " + recognizedText);
									// 识别出的文本走现有流程
									String finalText = recognizedText;
									executor.submit(() -> {
										try {
											IntentRecognizer.IntentType intent = intentRecognizer.recognize(finalText);
											String reply;
											switch (intent) {
												case WEATHER -> reply = getWeatherReply(finalText, weatherClient, intentRecognizer);
												case HELP -> reply = getHelpReply();
												case VOICE_REPLY -> {
													replyByVoice[0] = true;
													reply = "✅ 已切换为语音回复模式";
												}
												case TEXT_REPLY -> {
													replyByVoice[0] = false;
													reply = "✅ 已切换为文字回复模式";
												}
												default -> reply = llmService.chat(finalText);
											}
											if (replyByVoice[0]) {
												byte[] mp3Bytes = ttsService.synthesize(reply);
												if (mp3Bytes != null) {
													client.sendFile(fromUserId, mp3Bytes, "语音回复.mp3", "" );
												} else {
													client.sendText(fromUserId, "🎤 识别到：" + finalText + "\n\n" + reply);
												}
											} else {
												client.sendText(fromUserId, "🎤 识别到：" + finalText + "\n\n" + reply);
											}
											System.out.println("[✅ 语音回复发送完成]");
										} catch (Exception e) {
											e.printStackTrace();
											try {
												client.sendText(fromUserId, "服务出错");
											} catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									});
								} else {
									client.sendText(fromUserId, "🎤 语音识别失败，请说清楚一点或改用文字");
								}
							} catch (Exception e) {
								e.printStackTrace();
								try {
									client.sendText(fromUserId, "语音处理出错");
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}
					}
				}
			} catch (Exception e) {
				System.err.println("[拉取异常] " + e.getMessage());
			}

			Thread.sleep(2000);
		}
	}

	/**
	 * 天气查询：提取城市 → 调API → 格式化回复
	 */
	private static String getWeatherReply(String text, WeatherClient weatherClient,IntentRecognizer recognizer) {
		// 1. 提取城市名
		String city = recognizer.extractCity(text);
		if (city == null) {
			city = "长沙";
		}

		// 2. 城市 → LocationID
		String locationId = switch (city) {
			case "北京" -> "101010100";
			case "上海" -> "101020100";
			case "南京" -> "101190101";
			case "广州" -> "101280101";
			case "深圳" -> "101280601";
			case "杭州" -> "101210101";
			case "成都" -> "101270101";
			default -> "101250101"; // 长沙
		};

		// 3. 调 API
		try {
			WeatherResponse resp = weatherClient.getNowWeather(locationId);
			if ("200".equals(resp.getCode()) && resp.getNow() != null) {
				WeatherResponse.Now n = resp.getNow();
				return city + "天气：" + n.getText()
						+ "，" + n.getTemp() + "°C（体感" + n.getFeelsLike() + "°C），"
						+ n.getWindDir() + n.getWindScale() + "级，湿度" + n.getHumidity() + "%";
			} else {
				return "天气查询失败，code=" + resp.getCode();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "天气查询异常：" + e.getMessage();
		}
	}
	private static String getHelpReply() {
		return """
        🤖 我是智能微信助手，支持以下功能：

        🌤 天气查询
           发送「长沙天气」「今天多少度」「明天会下雨吗」「冷不冷」
           支持城市：北京/上海/广州/深圳/长沙/南京/杭州/成都等

        💬 闲聊对话
           随便聊，问我问题都行

        🖼️ 图片识别
           发图片给我，我会描述图片内容

        📋 发送「帮助」查看此菜单
        """;
	}
}