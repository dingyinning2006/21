package com.example.demo.wechat;

import com.example.demo.router.MessageRouter;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;

import java.util.List;

public class SimpleMessageHandler implements MessageHandler {

    private final WeChatClient client;
    private final MessageRouter messageRouter;

    // 构造方法注入 MessageRouter（总路由）
    public SimpleMessageHandler(WeChatClient client, MessageRouter messageRouter) {
        this.client = client;
        this.messageRouter = messageRouter;
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

                    // 所有文本消息统一走 MessageRouter 总路由
                    // 优先级：Skill（关键词确定性执行）→ RAG（知识库增强）→ LLM 兜底
                    String reply = messageRouter.route(text);
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