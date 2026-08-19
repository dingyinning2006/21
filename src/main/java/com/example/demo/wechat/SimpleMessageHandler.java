package com.example.demo.wechat;

import com.example.demo.service.FunctionCallingService;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;

import java.util.List;

public class SimpleMessageHandler implements MessageHandler {

    private final WeChatClient client;
    private final FunctionCallingService functionCallingService;

    // 构造方法多加一个 FunctionCallingService
    public SimpleMessageHandler(WeChatClient client, FunctionCallingService functionCallingService) {
        this.client = client;
        this.functionCallingService = functionCallingService;
    }

    @Override
    public void handle(List<WeixinMessage> messages) {
        for (WeixinMessage msg : messages) {
            String fromUserId = msg.getFrom_user_id();
            System.out.println("收到消息 from = " + fromUserId);

            if (msg.getItem_list() == null) continue;

            for (MessageItem item : msg.getItem_list()) {
                if (item.getText_item() != null) {
                    String text = item.getText_item().getText();
                    System.out.println("文本内容: " + text);

                    // 所有文本消息统一走 Function Calling
                    // LLM 自动判断：要不要调工具、调哪个、传什么参数
                    String reply = functionCallingService.chat(text);
                    System.out.println("回复: " + reply);

                    try {
                        client.sendText(fromUserId, reply);
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}